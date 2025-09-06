package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequestDto;
import com.example.bankcards.dto.request.TransferRequestDto;
import com.example.bankcards.dto.response.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card Controller", description = "Операции с картами (ADMIN и USER)")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать карту", description = "Доступно только ADMIN")
    public CardResponseDto createCard(@Valid @RequestBody CardCreateRequestDto request) {
        return cardService.createCard(request);
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Заблокировать карту", description = "Доступно только ADMIN")
    public CardResponseDto blockCard(@PathVariable("id") Long id) {
        return toDto(cardService.blockCard(id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Активировать карту", description = "Доступно только ADMIN")
    public CardResponseDto activateCard(@PathVariable("id") Long id) {
        return toDto(cardService.activateCard(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить карту", description = "Доступно только ADMIN")
    public void deleteCard(@PathVariable("id") Long id) {
        cardService.deleteCard(id);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить все карты (с пагинацией)", description = "Доступно только ADMIN")
    public Page<CardResponseDto> getAllCards(Pageable pageable) {
        return cardService.getAllCards(pageable).map(this::toDto);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Мои карты (список)", description = "USER может получить только свои карты")
    public List<CardResponseDto> getMyCards(Authentication authentication) {
        return cardService.getMyCards(authentication).stream().map(this::toDto).toList();
    }

    @GetMapping("/my/paged")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Мои карты (с пагинацией)", description = "USER может получить только свои карты")
    public Page<CardResponseDto> getMyCardsPaged(Authentication authentication, Pageable pageable) {
        return cardService.getMyCardsPaged(authentication, pageable).map(this::toDto);
    }

    @PostMapping("/{id}/request-block")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Запрос на блокировку карты", description = "USER может подать запрос на блокировку своей карты")
    public CardResponseDto requestBlockCard(@PathVariable Long id, Authentication authentication) {
        return toDto(cardService.requestBlockCard(id, authentication));
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Получить баланс карты", description = "USER может видеть баланс своей карты")
    public ResponseEntity<BigDecimal> getCardBalance(@PathVariable Long id, Authentication authentication) {
        BigDecimal balance = cardService.getCardBalance(id, authentication);
        return ResponseEntity.ok(balance != null ? balance : BigDecimal.ZERO);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Перевод между картами", description = "USER может переводить средства между своими картами")
    public void transfer(@Valid @RequestBody TransferRequestDto request, Authentication authentication) {
        cardService.transferBetweenCards(
                request.getFromCardId(),
                request.getToCardId(),
                request.getAmount(),
                authentication
        );
    }

    private CardResponseDto toDto(Card card) {
        CardResponseDto dto = new CardResponseDto();
        dto.setId(card.getId());
        dto.setMaskedNumber(cardService.maskCardNumber(card.getNumber()));
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(card.getStatus());
        dto.setBalance(card.getBalance());
        return dto;
    }
}
