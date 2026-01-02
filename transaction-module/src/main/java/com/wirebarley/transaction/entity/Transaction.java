package com.wirebarley.transaction.entity;

import com.wirebarley.account.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_owner_account", columnList = "owner_account_id"),
        @Index(name = "idx_transaction_from_account", columnList = "from_account_id"),
        @Index(name = "idx_transaction_to_account", columnList = "to_account_id"),
        @Index(name = "idx_transaction_created_at", columnList = "createdAt"),
        @Index(name = "idx_transaction_idempotency_key", columnList = "idempotencyKey")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 15, scale = 2)
    private BigDecimal fee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id", nullable = false)
    private Account ownerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 64, unique = true)
    private String idempotencyKey;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Transaction(TransactionType type, BigDecimal amount, BigDecimal fee,
                       Account ownerAccount, Account fromAccount, Account toAccount,
                       BigDecimal balanceAfter, TransactionStatus status, String idempotencyKey) {
        this.type = type;
        this.amount = amount;
        this.fee = fee;
        this.ownerAccount = ownerAccount;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.balanceAfter = balanceAfter;
        this.status = status != null ? status : TransactionStatus.SUCCESS;
        this.idempotencyKey = idempotencyKey;
    }
}
