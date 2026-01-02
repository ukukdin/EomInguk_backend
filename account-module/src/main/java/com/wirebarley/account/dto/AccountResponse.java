package com.wirebarley.account.dto;

import com.wirebarley.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String accountHolder;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolder(account.getAccountHolder())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
