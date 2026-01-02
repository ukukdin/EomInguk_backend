package com.wirebarley.account.service;

import com.wirebarley.account.dto.AccountRequest;
import com.wirebarley.account.dto.AccountResponse;
import com.wirebarley.account.entity.Account;
import com.wirebarley.account.repository.AccountRepository;
import com.wirebarley.common.exception.BusinessException;
import com.wirebarley.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 단위 테스트")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Nested
    @DisplayName("계좌 생성")
    class CreateAccount {

        @Test
        @DisplayName("성공: 유효한 요청으로 계좌 생성")
        void createAccount_Success() {
            // given
            AccountRequest request = AccountRequest.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .build();

            Account savedAccount = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(BigDecimal.ZERO)
                    .build();

            given(accountRepository.existsByAccountNumber(anyString())).willReturn(false);
            given(accountRepository.save(any(Account.class))).willReturn(savedAccount);

            // when
            AccountResponse response = accountService.createAccount(request);

            // then
            assertThat(response.getAccountNumber()).isEqualTo("1234567890");
            assertThat(response.getAccountHolder()).isEqualTo("홍길동");
            assertThat(response.getBalance()).isEqualTo(BigDecimal.ZERO);
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("실패: 중복된 계좌번호")
        void createAccount_DuplicateAccountNumber() {
            // given
            AccountRequest request = AccountRequest.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .build();

            given(accountRepository.existsByAccountNumber("1234567890")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> accountService.createAccount(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_ACCOUNT_NUMBER);

            verify(accountRepository, never()).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("계좌 삭제")
    class DeleteAccount {

        @Test
        @DisplayName("성공: 잔액이 0인 계좌 삭제")
        void deleteAccount_Success() {
            // given
            Account account = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(BigDecimal.ZERO)
                    .build();

            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            // when
            accountService.deleteAccount(1L);

            // then
            verify(accountRepository).delete(account);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌")
        void deleteAccount_NotFound() {
            // given
            given(accountRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.deleteAccount(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);

            verify(accountRepository, never()).delete(any(Account.class));
        }

        @Test
        @DisplayName("실패: 잔액이 있는 계좌 삭제 시도")
        void deleteAccount_HasBalance() {
            // given
            Account account = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("10000"))
                    .build();

            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            // when & then
            assertThatThrownBy(() -> accountService.deleteAccount(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_HAS_BALANCE);

            verify(accountRepository, never()).delete(any(Account.class));
        }
    }

    @Nested
    @DisplayName("계좌 조회")
    class GetAccount {

        @Test
        @DisplayName("성공: ID로 계좌 조회")
        void getAccount_ById_Success() {
            // given
            Account account = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("50000"))
                    .build();

            given(accountRepository.findById(1L)).willReturn(Optional.of(account));

            // when
            AccountResponse response = accountService.getAccount(1L);

            // then
            assertThat(response.getAccountNumber()).isEqualTo("1234567890");
            assertThat(response.getAccountHolder()).isEqualTo("홍길동");
            assertThat(response.getBalance()).isEqualTo(new BigDecimal("50000"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 ID")
        void getAccount_ById_NotFound() {
            // given
            given(accountRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.getAccount(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        }

        @Test
        @DisplayName("성공: 계좌번호로 조회")
        void getAccount_ByNumber_Success() {
            // given
            Account account = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("50000"))
                    .build();

            given(accountRepository.findByAccountNumber("1234567890")).willReturn(Optional.of(account));

            // when
            AccountResponse response = accountService.getAccountByNumber("1234567890");

            // then
            assertThat(response.getAccountNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌번호")
        void getAccount_ByNumber_NotFound() {
            // given
            given(accountRepository.findByAccountNumber("9999999999")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> accountService.getAccountByNumber("9999999999"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("전체 계좌 조회")
    class GetAllAccounts {

        @Test
        @DisplayName("성공: 모든 계좌 조회")
        void getAllAccounts_Success() {
            // given
            Account account1 = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(BigDecimal.ZERO)
                    .build();

            Account account2 = Account.builder()
                    .accountNumber("0987654321")
                    .accountHolder("김철수")
                    .balance(new BigDecimal("100000"))
                    .build();

            given(accountRepository.findAll()).willReturn(Arrays.asList(account1, account2));

            // when
            List<AccountResponse> responses = accountService.getAllAccounts();

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getAccountHolder()).isEqualTo("홍길동");
            assertThat(responses.get(1).getAccountHolder()).isEqualTo("김철수");
        }

        @Test
        @DisplayName("성공: 계좌가 없는 경우 빈 리스트 반환")
        void getAllAccounts_Empty() {
            // given
            given(accountRepository.findAll()).willReturn(List.of());

            // when
            List<AccountResponse> responses = accountService.getAllAccounts();

            // then
            assertThat(responses).isEmpty();
        }
    }
}
