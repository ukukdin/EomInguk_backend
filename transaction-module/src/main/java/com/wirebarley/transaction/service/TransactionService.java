package com.wirebarley.transaction.service;

import com.wirebarley.account.entity.Account;
import com.wirebarley.account.repository.AccountRepository;
import com.wirebarley.common.exception.BusinessException;
import com.wirebarley.common.exception.ErrorCode;
import com.wirebarley.transaction.dto.*;
import com.wirebarley.transaction.entity.Transaction;
import com.wirebarley.transaction.entity.TransactionStatus;
import com.wirebarley.transaction.entity.TransactionType;
import com.wirebarley.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private static final BigDecimal DAILY_WITHDRAWAL_LIMIT = new BigDecimal("1000000");
    private static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("3000000");
    private static final BigDecimal TRANSFER_FEE_RATE = new BigDecimal("0.01");

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        // 멱등성 체크
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return TransactionResponse.from(existing.get());
            }
        }

        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.deposit(request.getAmount());

        Transaction transaction = Transaction.builder()
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .ownerAccount(account)
                .toAccount(account)
                .balanceAfter(account.getBalance())
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionResponse.from(savedTransaction);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        // 멱등성 체크
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return TransactionResponse.from(existing.get());
            }
        }

        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateWithdrawalLimit(account, request.getAmount());

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        account.withdraw(request.getAmount());

        Transaction transaction = Transaction.builder()
                .type(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .ownerAccount(account)
                .fromAccount(account)
                .balanceAfter(account.getBalance())
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionResponse.from(savedTransaction);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        // 멱등성 체크
        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return TransactionResponse.from(existing.get());
            }
        }

        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        // 데드락 방지: 계좌번호 순서대로 락 획득
        String firstLock = request.getFromAccountNumber().compareTo(request.getToAccountNumber()) < 0
                ? request.getFromAccountNumber() : request.getToAccountNumber();
        String secondLock = request.getFromAccountNumber().compareTo(request.getToAccountNumber()) < 0
                ? request.getToAccountNumber() : request.getFromAccountNumber();

        Account first = accountRepository.findByAccountNumberWithLock(firstLock)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account second = accountRepository.findByAccountNumberWithLock(secondLock)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        Account fromAccount = first.getAccountNumber().equals(request.getFromAccountNumber()) ? first : second;
        Account toAccount = first.getAccountNumber().equals(request.getToAccountNumber()) ? first : second;

        validateTransferLimit(fromAccount, request.getAmount());

        BigDecimal fee = request.getAmount().multiply(TRANSFER_FEE_RATE).setScale(0, RoundingMode.DOWN);
        BigDecimal totalDeduction = request.getAmount().add(fee);

        if (fromAccount.getBalance().compareTo(totalDeduction) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        fromAccount.withdraw(totalDeduction);
        toAccount.deposit(request.getAmount());

        // 출금자 관점 거래 기록 (TRANSFER_OUT)
        Transaction outTransaction = Transaction.builder()
                .type(TransactionType.TRANSFER_OUT)
                .amount(request.getAmount())
                .fee(fee)
                .ownerAccount(fromAccount)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .balanceAfter(fromAccount.getBalance())
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        // 수취자 관점 거래 기록 (TRANSFER_IN)
        Transaction inTransaction = Transaction.builder()
                .type(TransactionType.TRANSFER_IN)
                .amount(request.getAmount())
                .ownerAccount(toAccount)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .balanceAfter(toAccount.getBalance())
                .status(TransactionStatus.SUCCESS)
                .build();

        transactionRepository.save(outTransaction);
        transactionRepository.save(inTransaction);

        return TransactionResponse.from(outTransaction);
    }

    public Page<TransactionResponse> getTransactionHistory(Long accountId, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        return transactionRepository.findByOwnerAccountOrderByCreatedAtDesc(account, pageable)
                .map(TransactionResponse::from);
    }

    public Page<TransactionResponse> getTransactionHistoryByAccountNumber(String accountNumber, Pageable pageable) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        return transactionRepository.findByOwnerAccountOrderByCreatedAtDesc(account, pageable)
                .map(TransactionResponse::from);
    }

    private void validateWithdrawalLimit(Account account, BigDecimal amount) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        BigDecimal dailyWithdrawal = transactionRepository.sumDailyAmountByOwnerAccountAndType(
                account, TransactionType.WITHDRAWAL, startOfDay, endOfDay);

        if (dailyWithdrawal.add(amount).compareTo(DAILY_WITHDRAWAL_LIMIT) > 0) {
            throw new BusinessException(ErrorCode.DAILY_WITHDRAWAL_LIMIT_EXCEEDED);
        }
    }

    private void validateTransferLimit(Account account, BigDecimal amount) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        BigDecimal dailyTransfer = transactionRepository.sumDailyAmountByOwnerAccountAndType(
                account, TransactionType.TRANSFER_OUT, startOfDay, endOfDay);

        if (dailyTransfer.add(amount).compareTo(DAILY_TRANSFER_LIMIT) > 0) {
            throw new BusinessException(ErrorCode.DAILY_TRANSFER_LIMIT_EXCEEDED);
        }
    }
}
