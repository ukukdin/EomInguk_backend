package com.wirebarley.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class DepositRequest {

    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNumber;

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "1", message = "입금 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    private String idempotencyKey;

    @Builder
    public DepositRequest(String accountNumber, BigDecimal amount, String idempotencyKey) {
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}
