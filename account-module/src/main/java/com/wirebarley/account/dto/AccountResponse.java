package com.wirebarley.account.dto;

import com.wirebarley.account.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "계좌 응답")
@Getter
@Builder
public class AccountResponse {

    @Schema(description = "계좌 ID", example = "1")
    private Long id;

    @Schema(description = "계좌번호", example = "1234567890")
    private String accountNumber;

    @Schema(description = "예금주명", example = "홍길동")
    private String accountHolder;

    @Schema(description = "현재 잔액", example = "100000.00")
    private BigDecimal balance;

    @Schema(description = "계좌 생성일시", example = "2024-01-02T10:30:00")
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
