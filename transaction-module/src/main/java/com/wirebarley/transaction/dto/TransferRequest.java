package com.wirebarley.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "이체 요청")
@Getter
@NoArgsConstructor
public class TransferRequest {

    @Schema(description = "출금 계좌번호 (보내는 계좌)", example = "1234567890", required = true)
    @NotBlank(message = "출금 계좌번호는 필수입니다.")
    private String fromAccountNumber;

    @Schema(description = "입금 계좌번호 (받는 계좌)", example = "0987654321", required = true)
    @NotBlank(message = "입금 계좌번호는 필수입니다.")
    private String toAccountNumber;

    @Schema(description = "이체 금액 (1원 이상, 일일 한도 3,000,000원, 수수료 1% 별도)", example = "10000", required = true)
    @NotNull(message = "금액은 필수입니다.")
    @DecimalMin(value = "1", message = "이체 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    @Schema(description = "멱등성 키 (중복 요청 방지용, 선택)", example = "transfer-uuid-12345")
    private String idempotencyKey;

    @Builder
    public TransferRequest(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String idempotencyKey) {
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }
}
