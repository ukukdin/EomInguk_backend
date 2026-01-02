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
public class TransferRequest {

    @NotBlank(message = "출금 계좌번호는 필수입니다.")
    private String fromAccountNumber;

    @NotBlank(message = "입금 계좌번호는 필수입니다.")
    private String toAccountNumber;

    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "1", message = "이체 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    private String idempotencyKey;

    @Builder
    public TransferRequest(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String idempotencyKey) {
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}
