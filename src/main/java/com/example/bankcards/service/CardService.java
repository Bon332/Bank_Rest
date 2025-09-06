package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequestDto;
import com.example.bankcards.dto.response.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ApiErrorException;
import com.example.bankcards.exception.constant.ErrorStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public CardResponseDto createCard(CardCreateRequestDto request) {
        User user = getUserOrThrow(request.getUserId());

        cardRepository.findByNumber(request.getNumber())
                .ifPresent(c -> { throw new ApiErrorException(ErrorStatus.VALIDATION_ERROR); });

        Card card = new Card();
        card.setUser(user);
        card.setNumber(request.getNumber());
        card.setExpiryDate(request.getExpiryDate());
        card.setBalance(BigDecimal.ZERO);

        if (request.getExpiryDate() != null && request.getExpiryDate().isBefore(LocalDate.now())) {
            card.setStatus(CardStatus.EXPIRED);
        } else {
            card.setStatus(CardStatus.ACTIVE);
        }

        cardRepository.save(card);

        return new CardResponseDto(
                card.getId(),
                maskCardNumber(card.getNumber()),
                card.getExpiryDate(),
                card.getStatus(),
                card.getBalance()
        );
    }

    public Card blockCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        card.setStatus(CardStatus.BLOCKED);
        return cardRepository.save(card);
    }

    public Card activateCard(Long cardId) {
        Card card = getCardOrThrow(cardId);
        if (isExpired(card)) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
            throw new ApiErrorException(ErrorStatus.FORBIDDEN_OPERATION);
        }
        card.setStatus(CardStatus.ACTIVE);
        return cardRepository.save(card);
    }

    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new ApiErrorException(ErrorStatus.CARD_NOT_FOUND);
        }
        cardRepository.deleteById(cardId);
    }

    public Page<Card> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    public List<Card> getMyCards(Authentication authentication) {
        User user = getUserByUsername(authentication.getName());
        return cardRepository.findByUser(user);
    }

    public Page<Card> getMyCardsPaged(Authentication authentication, Pageable pageable) {
        User user = getUserByUsername(authentication.getName());
        return cardRepository.findByUser(user, pageable);
    }

    public Card requestBlockCard(Long cardId, Authentication authentication) {
        User user = getUserByUsername(authentication.getName());
        Card card = getCardOrThrow(cardId);
        ensureOwner(card, user);

        if (card.getStatus() == CardStatus.BLOCKED || isExpired(card)) {
            throw new ApiErrorException(ErrorStatus.FORBIDDEN_OPERATION);
        }

        card.setStatus(CardStatus.REQUESTED_BLOCK);
        return cardRepository.save(card);
    }

    public BigDecimal getCardBalance(Long cardId, Authentication authentication) {
        User user = getUserByUsername(authentication.getName());
        Card card = getCardOrThrow(cardId);
        ensureOwner(card, user);

        return card.getBalance() != null ? card.getBalance() : BigDecimal.ZERO;
    }

    @Transactional
    public void transferBetweenCards(Long fromCardId, Long toCardId, BigDecimal amount, Authentication authentication) {
        User user = getUserByUsername(authentication.getName());

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiErrorException(ErrorStatus.VALIDATION_ERROR);
        }

        Card from = getCardOrThrow(fromCardId);
        Card to = getCardOrThrow(toCardId);

        ensureOwner(from, user);
        ensureOwner(to, user);

        failIfNotActiveOrExpired(from);
        failIfNotActiveOrExpired(to);

        if (from.getBalance().compareTo(amount) < 0) {
            throw new ApiErrorException(ErrorStatus.INSUFFICIENT_FUNDS);
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        cardRepository.save(from);
        cardRepository.save(to);
    }

    private Card getCardOrThrow(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ApiErrorException(ErrorStatus.CARD_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiErrorException(ErrorStatus.USER_NOT_FOUND));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiErrorException(ErrorStatus.USER_NOT_FOUND));
    }

    private void ensureOwner(Card card, User user) {
        if (card.getUser() == null || !card.getUser().getId().equals(user.getId())) {
            throw new ApiErrorException(ErrorStatus.FORBIDDEN_OPERATION);
        }
    }

    private boolean isExpired(Card card) {
        return card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now());
    }

    private void failIfNotActiveOrExpired(Card card) {
        if (isExpired(card)) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
            throw new ApiErrorException(ErrorStatus.FORBIDDEN_OPERATION);
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new ApiErrorException(ErrorStatus.FORBIDDEN_OPERATION);
        }
    }

    public String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
