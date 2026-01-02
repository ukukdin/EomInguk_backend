package com.wirebarley.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirebarley.common.exception.BusinessException;
import com.wirebarley.common.exception.ErrorCode;
import com.wirebarley.common.exception.GlobalExceptionHandler;
import com.wirebarley.transaction.dto.*;
import com.wirebarley.transaction.entity.TransactionType;
import com.wirebarley.transaction.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("TransactionController 테스트")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Nested
    @DisplayName("POST /api/transactions/deposit - 입금")
    class Deposit {

        @Test
        @DisplayName("성공: 유효한 입금 요청 시 200 OK 반환")
        void deposit_Success() throws Exception {
            // given
            DepositRequest request = DepositRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("10000"))
                    .build();

            TransactionResponse response = TransactionResponse.builder()
                    .id(1L)
                    .type(TransactionType.DEPOSIT)
                    .amount(new BigDecimal("10000"))
                    .toAccountNumber("1234567890")
                    .balanceAfter(new BigDecimal("110000"))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(transactionService.deposit(any(DepositRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.type").value("DEPOSIT"))
                    .andExpect(jsonPath("$.amount").value(10000))
                    .andExpect(jsonPath("$.balanceAfter").value(110000));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌 입금 시 404 Not Found 반환")
        void deposit_AccountNotFound() throws Exception {
            // given
            DepositRequest request = DepositRequest.builder()
                    .accountNumber("9999999999")
                    .amount(new BigDecimal("10000"))
                    .build();

            given(transactionService.deposit(any(DepositRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("A001"));
        }

        @Test
        @DisplayName("실패: 빈 계좌번호 시 400 Bad Request 반환")
        void deposit_EmptyAccountNumber() throws Exception {
            // given
            String requestJson = """
                    {
                        "accountNumber": "",
                        "amount": 10000
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.accountNumber").exists());
        }

        @Test
        @DisplayName("실패: 0원 이하 금액 시 400 Bad Request 반환")
        void deposit_InvalidAmount() throws Exception {
            // given
            String requestJson = """
                    {
                        "accountNumber": "1234567890",
                        "amount": 0
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.amount").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/withdraw - 출금")
    class Withdraw {

        @Test
        @DisplayName("성공: 유효한 출금 요청 시 200 OK 반환")
        void withdraw_Success() throws Exception {
            // given
            WithdrawRequest request = WithdrawRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("50000"))
                    .build();

            TransactionResponse response = TransactionResponse.builder()
                    .id(1L)
                    .type(TransactionType.WITHDRAWAL)
                    .amount(new BigDecimal("50000"))
                    .fromAccountNumber("1234567890")
                    .balanceAfter(new BigDecimal("50000"))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(transactionService.withdraw(any(WithdrawRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.amount").value(50000));
        }

        @Test
        @DisplayName("실패: 잔액 부족 시 400 Bad Request 반환")
        void withdraw_InsufficientBalance() throws Exception {
            // given
            WithdrawRequest request = WithdrawRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("1000000"))
                    .build();

            given(transactionService.withdraw(any(WithdrawRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

            // when & then
            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("T001"))
                    .andExpect(jsonPath("$.message").value("잔액이 부족합니다."));
        }

        @Test
        @DisplayName("실패: 일일 출금 한도 초과 시 400 Bad Request 반환")
        void withdraw_DailyLimitExceeded() throws Exception {
            // given
            WithdrawRequest request = WithdrawRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("500000"))
                    .build();

            given(transactionService.withdraw(any(WithdrawRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED));

            // when & then
            mockMvc.perform(post("/api/transactions/withdraw")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("T002"))
                    .andExpect(jsonPath("$.message").value("일일 출금 한도를 초과했습니다."));
        }
    }

    @Nested
    @DisplayName("POST /api/transactions/transfer - 이체")
    class Transfer {

        @Test
        @DisplayName("성공: 유효한 이체 요청 시 수수료 포함하여 200 OK 반환")
        void transfer_Success() throws Exception {
            // given
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("100000"))
                    .build();

            TransactionResponse response = TransactionResponse.builder()
                    .id(1L)
                    .type(TransactionType.TRANSFER_OUT)
                    .amount(new BigDecimal("100000"))
                    .fee(new BigDecimal("1000"))
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .balanceAfter(new BigDecimal("899000"))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(transactionService.transfer(any(TransferRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("TRANSFER_OUT"))
                    .andExpect(jsonPath("$.amount").value(100000))
                    .andExpect(jsonPath("$.fee").value(1000))
                    .andExpect(jsonPath("$.balanceAfter").value(899000));
        }

        @Test
        @DisplayName("실패: 잔액 부족 시 400 Bad Request 반환")
        void transfer_InsufficientBalance() throws Exception {
            // given
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("10000000"))
                    .build();

            given(transactionService.transfer(any(TransferRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.INSUFFICIENT_BALANCE));

            // when & then
            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("T001"));
        }

        @Test
        @DisplayName("실패: 일일 이체 한도 초과 시 400 Bad Request 반환")
        void transfer_DailyLimitExceeded() throws Exception {
            // given
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("2000000"))
                    .build();

            given(transactionService.transfer(any(TransferRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED));

            // when & then
            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("T003"))
                    .andExpect(jsonPath("$.message").value("일일 이체 한도를 초과했습니다."));
        }

        @Test
        @DisplayName("실패: 동일 계좌 이체 시 400 Bad Request 반환")
        void transfer_SameAccount() throws Exception {
            // given
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("1234567890")
                    .amount(new BigDecimal("10000"))
                    .build();

            given(transactionService.transfer(any(TransferRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER));

            // when & then
            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("T005"))
                    .andExpect(jsonPath("$.message").value("동일 계좌로 이체할 수 없습니다."));
        }

        @Test
        @DisplayName("실패: 빈 출금 계좌번호 시 400 Bad Request 반환")
        void transfer_EmptyFromAccountNumber() throws Exception {
            // given
            String requestJson = """
                    {
                        "fromAccountNumber": "",
                        "toAccountNumber": "0987654321",
                        "amount": 10000
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.fromAccountNumber").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/account/{accountId} - 거래내역 조회")
    class GetTransactionHistory {

        @Test
        @DisplayName("성공: 거래내역 조회 시 페이징 결과 반환")
        void getTransactionHistory_Success() throws Exception {
            // given
            List<TransactionResponse> transactions = List.of(
                    TransactionResponse.builder()
                            .id(2L)
                            .type(TransactionType.WITHDRAWAL)
                            .amount(new BigDecimal("50000"))
                            .fromAccountNumber("1234567890")
                            .balanceAfter(new BigDecimal("50000"))
                            .createdAt(LocalDateTime.now())
                            .build(),
                    TransactionResponse.builder()
                            .id(1L)
                            .type(TransactionType.DEPOSIT)
                            .amount(new BigDecimal("100000"))
                            .toAccountNumber("1234567890")
                            .balanceAfter(new BigDecimal("100000"))
                            .createdAt(LocalDateTime.now().minusHours(1))
                            .build()
            );

            Page<TransactionResponse> page = new PageImpl<>(
                    transactions,
                    PageRequest.of(0, 20),
                    2
            );

            given(transactionService.getTransactionHistory(eq(1L), any(Pageable.class)))
                    .willReturn(page);

            // when & then
            mockMvc.perform(get("/api/transactions/account/1")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].type").value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.content[1].type").value("DEPOSIT"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("성공: 거래내역이 없을 때 빈 페이지 반환")
        void getTransactionHistory_Empty() throws Exception {
            // given
            Page<TransactionResponse> emptyPage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 20),
                    0
            );

            given(transactionService.getTransactionHistory(eq(1L), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when & then
            mockMvc.perform(get("/api/transactions/account/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌 조회 시 404 Not Found 반환")
        void getTransactionHistory_AccountNotFound() throws Exception {
            // given
            given(transactionService.getTransactionHistory(eq(999L), any(Pageable.class)))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/transactions/account/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("A001"));
        }
    }

    @Nested
    @DisplayName("GET /api/transactions/account/number/{accountNumber} - 계좌번호로 거래내역 조회")
    class GetTransactionHistoryByAccountNumber {

        @Test
        @DisplayName("성공: 계좌번호로 거래내역 조회 시 페이징 결과 반환")
        void getTransactionHistoryByAccountNumber_Success() throws Exception {
            // given
            List<TransactionResponse> transactions = List.of(
                    TransactionResponse.builder()
                            .id(1L)
                            .type(TransactionType.DEPOSIT)
                            .amount(new BigDecimal("100000"))
                            .toAccountNumber("1234567890")
                            .balanceAfter(new BigDecimal("100000"))
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            Page<TransactionResponse> page = new PageImpl<>(
                    transactions,
                    PageRequest.of(0, 20),
                    1
            );

            given(transactionService.getTransactionHistoryByAccountNumber(eq("1234567890"), any(Pageable.class)))
                    .willReturn(page);

            // when & then
            mockMvc.perform(get("/api/transactions/account/number/1234567890"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].toAccountNumber").value("1234567890"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌번호 조회 시 404 Not Found 반환")
        void getTransactionHistoryByAccountNumber_NotFound() throws Exception {
            // given
            given(transactionService.getTransactionHistoryByAccountNumber(eq("9999999999"), any(Pageable.class)))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/transactions/account/number/9999999999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("A001"));
        }
    }
}
