package com.wirebarley.transaction.dto;

import com.wirebarley.transaction.entity.Transaction;
import com.wirebarley.transaction.entity.TransactionStatus;
import com.wirebarley.transaction.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "거래 응답")
@Getter
@Builder
public class TransactionResponse {

    @Schema(description = "거래 ID", example = "1")
    private Long id;

    @Schema(description = "거래 유형", example = "DEPOSIT",
            allowableValues = {"DEPOSIT", "WITHDRAWAL", "TRANSFER_OUT", "TRANSFER_IN"})
    private TransactionType type;

    @Schema(description = "거래 금액", example = "100000.00")
    private BigDecimal amount;

    @Schema(description = "수수료 (이체 시에만 발생, 이체 금액의 1%)", example = "1000.00")
    private BigDecimal fee;

    @Schema(description = "출금 계좌번호 (입금 시 null)", example = "1234567890")
    private String fromAccountNumber;

    @Schema(description = "입금 계좌번호 (출금 시 null)", example = "0987654321")
    private String toAccountNumber;

    @Schema(description = "거래 후 잔액", example = "99000.00")
    private BigDecimal balanceAfter;

    @Schema(description = "거래 상태", example = "SUCCESS",
            allowableValues = {"PENDING", "SUCCESS", "FAILED", "CANCELLED"})
    private TransactionStatus status;

    @Schema(description = "거래 일시", example = "2024-01-02T10:30:00")
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .fee(transaction.getFee())
                .fromAccountNumber(transaction.getFromAccount() != null ?
                        transaction.getFromAccount().getAccountNumber() : null)
                .toAccountNumber(transaction.getToAccount() != null ?
                        transaction.getToAccount().getAccountNumber() : null)
                .balanceAfter(transaction.getBalanceAfter())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
