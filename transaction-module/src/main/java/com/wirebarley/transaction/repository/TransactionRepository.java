package com.wirebarley.transaction.repository;

import com.wirebarley.account.entity.Account;
import com.wirebarley.transaction.entity.Transaction;
import com.wirebarley.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ownerAccount 기반 조회 (인덱스 최적화)
    Page<Transaction> findByOwnerAccountOrderByCreatedAtDesc(Account ownerAccount, Pageable pageable);

    Page<Transaction> findByOwnerAccountIdOrderByCreatedAtDesc(Long ownerAccountId, Pageable pageable);

    // 멱등성 키로 중복 체크
    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    // 일일 한도 계산 (ownerAccount 기반)
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.ownerAccount = :account " +
            "AND t.type = :type " +
            "AND t.status = 'SUCCESS' " +
            "AND t.createdAt >= :startOfDay " +
            "AND t.createdAt < :endOfDay")
    BigDecimal sumDailyAmountByOwnerAccountAndType(
            @Param("account") Account account,
            @Param("type") TransactionType type,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
