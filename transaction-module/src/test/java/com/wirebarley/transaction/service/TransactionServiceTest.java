package com.wirebarley.transaction.service;

import com.wirebarley.account.entity.Account;
import com.wirebarley.account.repository.AccountRepository;
import com.wirebarley.common.exception.BusinessException;
import com.wirebarley.common.exception.ErrorCode;
import com.wirebarley.transaction.dto.DepositRequest;
import com.wirebarley.transaction.dto.TransactionResponse;
import com.wirebarley.transaction.dto.TransferRequest;
import com.wirebarley.transaction.dto.WithdrawRequest;
import com.wirebarley.transaction.entity.Transaction;
import com.wirebarley.transaction.entity.TransactionType;
import com.wirebarley.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService 단위 테스트")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private Account testAccount2;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .accountNumber("1234567890")
                .accountHolder("홍길동")
                .balance(new BigDecimal("500000"))
                .build();

        testAccount2 = Account.builder()
                .accountNumber("0987654321")
                .accountHolder("김철수")
                .balance(new BigDecimal("100000"))
                .build();
    }

    @Nested
    @DisplayName("입금")
    class Deposit {

        @Test
        @DisplayName("성공: 유효한 금액 입금")
        void deposit_Success() {
            // given
            DepositRequest request = DepositRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("100000"))
                    .build();

            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));

            Transaction savedTransaction = Transaction.builder()
                    .type(TransactionType.DEPOSIT)
                    .amount(new BigDecimal("100000"))
                    .toAccount(testAccount)
                    .balanceAfter(new BigDecimal("600000"))
                    .build();

            given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

            // when
            TransactionResponse response = transactionService.deposit(request);

            // then
            assertThat(response.getType()).isEqualTo(TransactionType.DEPOSIT);
            assertThat(response.getAmount()).isEqualTo(new BigDecimal("100000"));
            assertThat(testAccount.getBalance()).isEqualTo(new BigDecimal("600000"));
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 계좌")
        void deposit_AccountNotFound() {
            // given
            DepositRequest request = DepositRequest.builder()
                    .accountNumber("9999999999")
                    .amount(new BigDecimal("100000"))
                    .build();

            given(accountRepository.findByAccountNumberWithLock("9999999999"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> transactionService.deposit(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("출금")
    class Withdraw {

        @Test
        @DisplayName("성공: 유효한 금액 출금")
        void withdraw_Success() {
            // given
            WithdrawRequest request = WithdrawRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("100000"))
                    .build();

            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.WITHDRAWAL), any(), any()))
                    .willReturn(BigDecimal.ZERO);

            Transaction savedTransaction = Transaction.builder()
                    .type(TransactionType.WITHDRAWAL)
                    .amount(new BigDecimal("100000"))
                    .fromAccount(testAccount)
                    .balanceAfter(new BigDecimal("400000"))
                    .build();

            given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

            // when
            TransactionResponse response = transactionService.withdraw(request);

            // then
            assertThat(response.getType()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(response.getAmount()).isEqualTo(new BigDecimal("100000"));
            assertThat(testAccount.getBalance()).isEqualTo(new BigDecimal("400000"));
        }

        @Test
        @DisplayName("실패: 잔액 부족")
        void withdraw_InsufficientBalance() {
            // given
            WithdrawRequest request = WithdrawRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("1000000"))
                    .build();

            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.WITHDRAWAL), any(), any()))
                    .willReturn(BigDecimal.ZERO);

            // when & then
            assertThatThrownBy(() -> transactionService.withdraw(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
        }

        @Test
        @DisplayName("실패: 일일 출금 한도 초과 (1,000,000원)")
        void withdraw_DailyLimitExceeded() {
            // given
            WithdrawRequest request = WithdrawRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("500000"))
                    .build();

            // 이미 600,000원 출금한 상태
            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.WITHDRAWAL), any(), any()))
                    .willReturn(new BigDecimal("600000"));

            // when & then
            assertThatThrownBy(() -> transactionService.withdraw(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("성공: 일일 한도 내 출금 (누적 1,000,000원)")
        void withdraw_WithinDailyLimit() {
            // given
            testAccount = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("2000000"))
                    .build();

            WithdrawRequest request = WithdrawRequest.builder()
                    .accountNumber("1234567890")
                    .amount(new BigDecimal("400000"))
                    .build();

            // 이미 600,000원 출금한 상태 → 400,000 추가하면 딱 1,000,000원
            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.WITHDRAWAL), any(), any()))
                    .willReturn(new BigDecimal("600000"));

            Transaction savedTransaction = Transaction.builder()
                    .type(TransactionType.WITHDRAWAL)
                    .amount(new BigDecimal("400000"))
                    .fromAccount(testAccount)
                    .balanceAfter(new BigDecimal("1600000"))
                    .build();

            given(transactionRepository.save(any(Transaction.class))).willReturn(savedTransaction);

            // when
            TransactionResponse response = transactionService.withdraw(request);

            // then
            assertThat(response.getAmount()).isEqualTo(new BigDecimal("400000"));
        }
    }

    @Nested
    @DisplayName("이체")
    class Transfer {

        @Test
        @DisplayName("성공: 유효한 이체 (수수료 1% 적용)")
        void transfer_Success() {
            // given
            testAccount = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("500000"))
                    .build();

            testAccount2 = Account.builder()
                    .accountNumber("0987654321")
                    .accountHolder("김철수")
                    .balance(new BigDecimal("100000"))
                    .build();

            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("100000"))
                    .build();

            given(accountRepository.findByAccountNumberWithLock("0987654321"))
                    .willReturn(Optional.of(testAccount2));
            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.TRANSFER_OUT), any(), any()))
                    .willReturn(BigDecimal.ZERO);

            Transaction outTransaction = Transaction.builder()
                    .type(TransactionType.TRANSFER_OUT)
                    .amount(new BigDecimal("100000"))
                    .fee(new BigDecimal("1000"))
                    .fromAccount(testAccount)
                    .toAccount(testAccount2)
                    .balanceAfter(new BigDecimal("398900"))
                    .build();

            given(transactionRepository.save(any(Transaction.class))).willReturn(outTransaction);

            // when
            TransactionResponse response = transactionService.transfer(request);

            // then
            assertThat(response.getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(response.getAmount()).isEqualTo(new BigDecimal("100000"));
            assertThat(response.getFee()).isEqualTo(new BigDecimal("1000")); // 1% 수수료
            // 출금: 100,000 + 1,000 (수수료) = 101,000
            assertThat(testAccount.getBalance()).isEqualTo(new BigDecimal("399000"));
            // 입금: 100,000
            assertThat(testAccount2.getBalance()).isEqualTo(new BigDecimal("200000"));
        }

        @Test
        @DisplayName("실패: 동일 계좌로 이체")
        void transfer_SameAccount() {
            // given
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("1234567890")
                    .amount(new BigDecimal("100000"))
                    .build();

            // when & then
            assertThatThrownBy(() -> transactionService.transfer(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        @Test
        @DisplayName("실패: 잔액 부족 (수수료 포함)")
        void transfer_InsufficientBalanceWithFee() {
            // given
            testAccount = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("100000"))
                    .build();

            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("100000")) // 100,000 + 1,000 수수료 = 101,000 필요
                    .build();

            given(accountRepository.findByAccountNumberWithLock("0987654321"))
                    .willReturn(Optional.of(testAccount2));
            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.TRANSFER_OUT), any(), any()))
                    .willReturn(BigDecimal.ZERO);

            // when & then
            assertThatThrownBy(() -> transactionService.transfer(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_BALANCE);
        }

        @Test
        @DisplayName("실패: 일일 이체 한도 초과 (3,000,000원)")
        void transfer_DailyLimitExceeded() {
            // given
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("1000000"))
                    .build();

            given(accountRepository.findByAccountNumberWithLock("0987654321"))
                    .willReturn(Optional.of(testAccount2));
            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            // 이미 2,500,000원 이체한 상태
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.TRANSFER_OUT), any(), any()))
                    .willReturn(new BigDecimal("2500000"));

            // when & then
            assertThatThrownBy(() -> transactionService.transfer(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("성공: 일일 이체 한도 내 (누적 3,000,000원)")
        void transfer_WithinDailyLimit() {
            // given
            testAccount = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("5000000"))
                    .build();

            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("1000000"))
                    .build();

            given(accountRepository.findByAccountNumberWithLock("0987654321"))
                    .willReturn(Optional.of(testAccount2));
            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            // 이미 2,000,000원 이체한 상태 → 1,000,000 추가하면 딱 3,000,000원
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.TRANSFER_OUT), any(), any()))
                    .willReturn(new BigDecimal("2000000"));

            Transaction outTransaction = Transaction.builder()
                    .type(TransactionType.TRANSFER_OUT)
                    .amount(new BigDecimal("1000000"))
                    .fee(new BigDecimal("10000"))
                    .fromAccount(testAccount)
                    .toAccount(testAccount2)
                    .balanceAfter(new BigDecimal("3990000"))
                    .build();

            given(transactionRepository.save(any(Transaction.class))).willReturn(outTransaction);

            // when
            TransactionResponse response = transactionService.transfer(request);

            // then
            assertThat(response.getAmount()).isEqualTo(new BigDecimal("1000000"));
            assertThat(response.getFee()).isEqualTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("수수료 계산: 버림 처리 확인")
        void transfer_FeeRoundingDown() {
            // given
            testAccount = Account.builder()
                    .accountNumber("1234567890")
                    .accountHolder("홍길동")
                    .balance(new BigDecimal("500000"))
                    .build();

            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber("1234567890")
                    .toAccountNumber("0987654321")
                    .amount(new BigDecimal("12345")) // 1% = 123.45 → 버림 → 123
                    .build();

            given(accountRepository.findByAccountNumberWithLock("0987654321"))
                    .willReturn(Optional.of(testAccount2));
            given(accountRepository.findByAccountNumberWithLock("1234567890"))
                    .willReturn(Optional.of(testAccount));
            given(transactionRepository.sumDailyAmountByOwnerAccountAndType(
                    any(Account.class), eq(TransactionType.TRANSFER_OUT), any(), any()))
                    .willReturn(BigDecimal.ZERO);

            Transaction outTransaction = Transaction.builder()
                    .type(TransactionType.TRANSFER_OUT)
                    .amount(new BigDecimal("12345"))
                    .fee(new BigDecimal("123")) // 버림 처리
                    .fromAccount(testAccount)
                    .toAccount(testAccount2)
                    .balanceAfter(new BigDecimal("487532"))
                    .build();

            given(transactionRepository.save(any(Transaction.class))).willReturn(outTransaction);

            // when
            TransactionResponse response = transactionService.transfer(request);

            // then
            assertThat(response.getFee()).isEqualTo(new BigDecimal("123"));
        }
    }
}
