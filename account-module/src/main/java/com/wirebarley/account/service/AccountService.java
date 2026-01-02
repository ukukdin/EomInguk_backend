package com.wirebarley.account.service;

import com.wirebarley.account.dto.AccountRequest;
import com.wirebarley.account.dto.AccountResponse;
import com.wirebarley.account.entity.Account;
import com.wirebarley.account.repository.AccountRepository;
import com.wirebarley.common.exception.BusinessException;
import com.wirebarley.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }

        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .accountHolder(request.getAccountHolder())
                .balance(BigDecimal.ZERO)
                .build();

        Account savedAccount = accountRepository.save(account);
        return AccountResponse.from(savedAccount);
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_HAS_BALANCE);
        }

        accountRepository.delete(account);
    }

    public AccountResponse getAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return AccountResponse.from(account);
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        return AccountResponse.from(account);
    }

    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }
}
