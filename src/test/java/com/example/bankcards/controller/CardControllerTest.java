package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequestDto;
import com.example.bankcards.dto.request.TransferRequestDto;
import com.example.bankcards.dto.response.CardResponseDto;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.ApiErrorException;
import com.example.bankcards.exception.constant.ErrorStatus;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class, excludeFilters = {
        @org.springframework.context.annotation.ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = com.example.bankcards.security.JwtAuthenticationFilter.class
        )
})
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    void createCard_Success() throws Exception {
        CardResponseDto response = new CardResponseDto(
                1L, "**** 3456", LocalDate.of(2030, 12, 31), CardStatus.ACTIVE, BigDecimal.ZERO
        );

        CardCreateRequestDto request = new CardCreateRequestDto(10L, "1234-5678-9012-3456", LocalDate.of(2030, 12, 31));

        when(cardService.createCard(any(CardCreateRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.maskedNumber").value("**** 3456"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createCard_Failure_DuplicateNumber() throws Exception {
        CardCreateRequestDto request = new CardCreateRequestDto(10L, "1234", LocalDate.of(2030, 12, 31));

        when(cardService.createCard(any(CardCreateRequestDto.class)))
                .thenThrow(new ApiErrorException(ErrorStatus.VALIDATION_ERROR));

        mockMvc.perform(post("/api/v1/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value(ErrorStatus.VALIDATION_ERROR.getMessage()));
    }

    @Test
    void blockCard_Success() throws Exception {
        CardResponseDto response = new CardResponseDto(1L, "**** 3456", LocalDate.now(), CardStatus.BLOCKED, BigDecimal.ZERO);
        when(cardService.blockCard(1L)).thenReturn(new com.example.bankcards.entity.Card());

        mockMvc.perform(post("/api/v1/cards/{id}/block", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void activateCard_Failure_Expired() throws Exception {
        when(cardService.activateCard(1L))
                .thenThrow(new ApiErrorException(ErrorStatus.FORBIDDEN_OPERATION));

        mockMvc.perform(post("/api/v1/cards/{id}/activate", 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value(ErrorStatus.FORBIDDEN_OPERATION.getMessage()));
    }

    @Test
    void deleteCard_Success() throws Exception {
        doNothing().when(cardService).deleteCard(1L);

        mockMvc.perform(delete("/api/v1/cards/{id}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void getAllCards_Success() throws Exception {
        List<com.example.bankcards.entity.Card> cards = List.of(
                new com.example.bankcards.entity.Card(1L, "1234", LocalDate.of(2030, 12, 31), CardStatus.ACTIVE, BigDecimal.ZERO, null),
                new com.example.bankcards.entity.Card(2L, "5678", LocalDate.of(2035, 5, 30), CardStatus.BLOCKED, BigDecimal.valueOf(500), null)
        );

        when(cardService.getAllCards(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(cards));

        mockMvc.perform(get("/api/v1/cards/all?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getMyCards_Success() throws Exception {
        when(cardService.getMyCards(any(Authentication.class)))
                .thenReturn(List.of(new com.example.bankcards.entity.Card()));

        mockMvc.perform(get("/api/v1/cards/my"))
                .andExpect(status().isOk());
    }

    @Test
    void requestBlockCard_Failure_Forbidden() throws Exception {
        doThrow(new ApiErrorException(ErrorStatus.FORBIDDEN_OPERATION))
                .when(cardService)
                .requestBlockCard(eq(1L), any());

        mockMvc.perform(post("/api/v1/cards/{id}/request-block", 1L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value(ErrorStatus.FORBIDDEN_OPERATION.getMessage()));
    }


    @Test
    void getCardBalance_Success() throws Exception {
        Authentication authentication = mock(Authentication.class);

        when(cardService.getCardBalance(eq(1L), eq(authentication)))
                .thenReturn(BigDecimal.valueOf(200));

        mockMvc.perform(get("/api/v1/cards/1/balance")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(content().string("200"));
    }



    @Test
    void transfer_Failure_InsufficientFunds() throws Exception {
        TransferRequestDto request = new TransferRequestDto(1L, 2L, BigDecimal.valueOf(500));

        doThrow(new ApiErrorException(ErrorStatus.INSUFFICIENT_FUNDS))
                .when(cardService)
                .transferBetweenCards(eq(1L), eq(2L), eq(BigDecimal.valueOf(500)), any());

        mockMvc.perform(post("/api/v1/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorMessage").value(ErrorStatus.INSUFFICIENT_FUNDS.getMessage()));
    }

}
