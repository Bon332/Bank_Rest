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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CardService cardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCard_userNotFound() {
        CardCreateRequestDto request = new CardCreateRequestDto(1L, "1234", LocalDate.now());
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.createCard(request));

        assertEquals(ErrorStatus.USER_NOT_FOUND, ex.getErrorStatus());
    }

    @Test
    void createCard_duplicateNumber() {
        CardCreateRequestDto request = new CardCreateRequestDto(1L, "1234", LocalDate.now());
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findByNumber("1234")).thenReturn(Optional.of(new Card()));

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.createCard(request));

        assertEquals(ErrorStatus.VALIDATION_ERROR, ex.getErrorStatus());
    }

    @Test
    void createCard_success() {
        CardCreateRequestDto request = new CardCreateRequestDto(1L, "1234", LocalDate.now().plusYears(1));
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findByNumber("1234")).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card card = inv.getArgument(0);
            card.setId(99L);
            return card;
        });

        CardResponseDto result = cardService.createCard(request);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(CardStatus.ACTIVE, result.getStatus());
    }

    @Test
    void blockCard_success() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.blockCard(1L);

        assertEquals(CardStatus.BLOCKED, result.getStatus());
    }

    @Test
    void blockCard_notFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.blockCard(1L));

        assertEquals(ErrorStatus.CARD_NOT_FOUND, ex.getErrorStatus());
    }

    @Test
    void activateCard_expired() {
        Card card = new Card();
        card.setId(1L);
        card.setExpiryDate(LocalDate.now().minusDays(1));

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.activateCard(1L));

        assertEquals(ErrorStatus.FORBIDDEN_OPERATION, ex.getErrorStatus());
        assertEquals(CardStatus.EXPIRED, card.getStatus());
    }

    @Test
    void activateCard_success() {
        Card card = new Card();
        card.setId(1L);
        card.setExpiryDate(LocalDate.now().plusDays(10));
        card.setStatus(CardStatus.BLOCKED);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        Card result = cardService.activateCard(1L);

        assertEquals(CardStatus.ACTIVE, result.getStatus());
    }

    @Test
    void deleteCard_notFound() {
        when(cardRepository.existsById(1L)).thenReturn(false);

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.deleteCard(1L));

        assertEquals(ErrorStatus.CARD_NOT_FOUND, ex.getErrorStatus());
    }

    @Test
    void deleteCard_success() {
        when(cardRepository.existsById(1L)).thenReturn(true);

        cardService.deleteCard(1L);

        verify(cardRepository, times(1)).deleteById(1L);
    }

    @Test
    void getAllCards_success() {
        Page<Card> page = new PageImpl<>(List.of(new Card()));
        when(cardRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Card> result = cardService.getAllCards(Pageable.unpaged());

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getMyCards_userNotFound() {
        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.getMyCards(authentication));

        assertEquals(ErrorStatus.USER_NOT_FOUND, ex.getErrorStatus());
    }

    @Test
    void getMyCards_success() {
        User user = new User();
        user.setId(1L);
        Card card = new Card();
        card.setUser(user);

        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByUser(user)).thenReturn(List.of(card));

        List<Card> result = cardService.getMyCards(authentication);

        assertEquals(1, result.size());
    }

    @Test
    void requestBlockCard_success() {
        User user = new User();
        user.setId(1L);
        Card card = new Card();
        card.setId(1L);
        card.setUser(user);
        card.setStatus(CardStatus.ACTIVE);

        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card result = cardService.requestBlockCard(1L, authentication);

        assertEquals(CardStatus.REQUESTED_BLOCK, result.getStatus());
    }

    @Test
    void requestBlockCard_forbidden_ifBlocked() {
        User user = new User();
        user.setId(1L);
        Card card = new Card();
        card.setId(1L);
        card.setUser(user);
        card.setStatus(CardStatus.BLOCKED);

        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.requestBlockCard(1L, authentication));

        assertEquals(ErrorStatus.FORBIDDEN_OPERATION, ex.getErrorStatus());
    }

    @Test
    void getCardBalance_success() {
        User user = new User();
        user.setId(1L);
        Card card = new Card();
        card.setUser(user);
        card.setBalance(BigDecimal.valueOf(123));

        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        BigDecimal balance = cardService.getCardBalance(1L, authentication);

        assertEquals(BigDecimal.valueOf(123), balance);
    }

    @Test
    void getCardBalance_returnsZeroIfNull() {
        User user = new User();
        user.setId(1L);
        Card card = new Card();
        card.setUser(user);
        card.setBalance(null);

        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        BigDecimal balance = cardService.getCardBalance(1L, authentication);

        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    void transfer_insufficientFunds() {
        User user = new User();
        user.setId(1L);
        Card from = new Card();
        from.setUser(user);
        from.setBalance(BigDecimal.valueOf(10));
        from.setStatus(CardStatus.ACTIVE);
        Card to = new Card();
        to.setUser(user);
        to.setBalance(BigDecimal.ZERO);
        to.setStatus(CardStatus.ACTIVE);

        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> cardService.transferBetweenCards(1L, 2L, BigDecimal.valueOf(100), authentication));

        assertEquals(ErrorStatus.INSUFFICIENT_FUNDS, ex.getErrorStatus());
    }

    @Test
    void transfer_success() {
        User user = new User();
        user.setId(1L);
        Card from = new Card();
        from.setUser(user);
        from.setBalance(BigDecimal.valueOf(200));
        from.setStatus(CardStatus.ACTIVE);
        Card to = new Card();
        to.setUser(user);
        to.setBalance(BigDecimal.ZERO);
        to.setStatus(CardStatus.ACTIVE);

        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        cardService.transferBetweenCards(1L, 2L, BigDecimal.valueOf(50), authentication);

        assertEquals(BigDecimal.valueOf(150), from.getBalance());
        assertEquals(BigDecimal.valueOf(50), to.getBalance());
    }
}
