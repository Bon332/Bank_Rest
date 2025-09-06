package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardResponseDto {
    private Long id;
    private String maskedNumber;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
}
