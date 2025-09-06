package com.example.bankcards.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateRequestDto {

    @NotNull(message = "UserId обязателен")
    private Long userId;

    @NotBlank(message = "Номер карты обязателен")
    private String number;

    @NotNull(message = "Срок действия обязателен")
    @Future(message = "Срок действия должен быть в будущем")
    private LocalDate expiryDate;
}
