package com.wirebarley.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "출금 요청")
@Getter
@NoArgsConstructor
public class WithdrawRequest {

    @Schema(description = "출금할 계좌번호", example = "1234567890", required = true)
    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNumber;

    @Schema(description = "출금 금액 (1원 이상, 일일 한도 1,000,000원)", example = "50000", required = true)
    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "1", message = "출금 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    @Schema(description = "멱등성 키 (중복 요청 방지용, 선택)", example = "withdraw-uuid-12345")
    private String idempotencyKey;

    @Builder
    public WithdrawRequest(String accountNumber, BigDecimal amount, String idempotencyKey) {
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}
