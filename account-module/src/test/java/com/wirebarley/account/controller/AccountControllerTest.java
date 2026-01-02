package com.wirebarley.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirebarley.account.dto.AccountRequest;
import com.wirebarley.account.dto.AccountResponse;
import com.wirebarley.account.service.AccountService;
import com.wirebarley.common.exception.BusinessException;
import com.wirebarley.common.exception.ErrorCode;
import com.wirebarley.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AccountController 테스트")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Nested
    @DisplayName("POST /api/accounts - 계좌 생성")
    class CreateAccount {

        @Test
        @DisplayName("성공: 유효한 요청 시 201 Created 반환")
        void createAccount_Success() throws Exception {
            // given
            AccountRequest request = AccountRequest.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .build();

            AccountResponse response = AccountResponse.builder()
                    .id(1L)
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(accountService.createAccount(any(AccountRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.accountHolder").value("홍길동"))
                    .andExpect(jsonPath("$.balance").value(0));
        }

        @Test
        @DisplayName("실패: 중복 계좌번호 시 409 Conflict 반환")
        void createAccount_DuplicateAccountNumber() throws Exception {
            // given
            AccountRequest request = AccountRequest.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .build();

            given(accountService.createAccount(any(AccountRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER));

            // when & then
            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("A002"))
                    .andExpect(jsonPath("$.message").value("이미 존재하는 계좌번호입니다."));
        }

        @Test
        @DisplayName("실패: 빈 계좌번호 시 400 Bad Request 반환")
        void createAccount_EmptyAccountNumber() throws Exception {
            // given
            String requestJson = """
                    {
                        "accountNumber": "",
                        "accountHolder": "홍길동"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"))
                    .andExpect(jsonPath("$.errors.accountNumber").exists());
        }

        @Test
        @DisplayName("실패: 잘못된 계좌번호 형식 시 400 Bad Request 반환")
        void createAccount_InvalidAccountNumberFormat() throws Exception {
            // given
            String requestJson = """
                    {
                        "accountNumber": "12345",
                        "accountHolder": "홍길동"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.accountNumber").exists());
        }

        @Test
        @DisplayName("실패: 빈 예금주명 시 400 Bad Request 반환")
        void createAccount_EmptyAccountHolder() throws Exception {
            // given
            String requestJson = """
                    {
                        "accountNumber": "1234567890",
                        "accountHolder": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/accounts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.accountHolder").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/accounts/{accountId} - 계좌 삭제")
    class DeleteAccount {

        @Test
        @DisplayName("성공: 잔액 0인 계좌 삭제 시 204 No Content 반환")
        void deleteAccount_Success() throws Exception {
            // given
            doNothing().when(accountService).deleteAccount(1L);

            // when & then
            mockMvc.perform(delete("/api/accounts/1"))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌 삭제 시 404 Not Found 반환")
        void deleteAccount_NotFound() throws Exception {
            // given
            doThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                    .when(accountService).deleteAccount(999L);

            // when & then
            mockMvc.perform(delete("/api/accounts/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("A001"));
        }

        @Test
        @DisplayName("실패: 잔액 있는 계좌 삭제 시 400 Bad Request 반환")
        void deleteAccount_HasBalance() throws Exception {
            // given
            doThrow(new BusinessException(ErrorCode.ACCOUNT_HAS_BALANCE))
                    .when(accountService).deleteAccount(1L);

            // when & then
            mockMvc.perform(delete("/api/accounts/1"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("A003"))
                    .andExpect(jsonPath("$.message").value("잔액이 있는 계좌는 삭제할 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("GET /api/accounts/{accountId} - 계좌 조회")
    class GetAccount {

        @Test
        @DisplayName("성공: ID로 계좌 조회 시 200 OK 반환")
        void getAccount_Success() throws Exception {
            // given
            AccountResponse response = AccountResponse.builder()
                    .id(1L)
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("100000"))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(accountService.getAccount(1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/accounts/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.balance").value(100000));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌 조회 시 404 Not Found 반환")
        void getAccount_NotFound() throws Exception {
            // given
            given(accountService.getAccount(999L))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/accounts/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("A001"));
        }
    }

    @Nested
    @DisplayName("GET /api/accounts/number/{accountNumber} - 계좌번호로 조회")
    class GetAccountByNumber {

        @Test
        @DisplayName("성공: 계좌번호로 조회 시 200 OK 반환")
        void getAccountByNumber_Success() throws Exception {
            // given
            AccountResponse response = AccountResponse.builder()
                    .id(1L)
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("50000"))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(accountService.getAccountByNumber("1234567890")).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/accounts/number/1234567890"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$.balance").value(50000));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌번호 조회 시 404 Not Found 반환")
        void getAccountByNumber_NotFound() throws Exception {
            // given
            given(accountService.getAccountByNumber("9999999999"))
                    .willThrow(new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/accounts/number/9999999999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("A001"));
        }
    }

    @Nested
    @DisplayName("GET /api/accounts - 전체 계좌 조회")
    class GetAllAccounts {

        @Test
        @DisplayName("성공: 전체 계좌 목록 조회 시 200 OK 반환")
        void getAllAccounts_Success() throws Exception {
            // given
            List<AccountResponse> responses = List.of(
                    AccountResponse.builder()
                            .id(1L)
                            .accountNumber("1234567890")
                            .accountHolder("홍길동")
                            .balance(new BigDecimal("100000"))
                            .createdAt(LocalDateTime.now())
                            .build(),
                    AccountResponse.builder()
                            .id(2L)
                            .accountNumber("0987654321")
                            .accountHolder("김철수")
                            .balance(new BigDecimal("200000"))
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            given(accountService.getAllAccounts()).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/accounts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].accountNumber").value("1234567890"))
                    .andExpect(jsonPath("$[1].accountNumber").value("0987654321"));
        }

        @Test
        @DisplayName("성공: 계좌가 없을 때 빈 배열 반환")
        void getAllAccounts_Empty() throws Exception {
            // given
            given(accountService.getAllAccounts()).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/accounts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }
}
