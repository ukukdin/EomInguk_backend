-- =====================================================
-- 송금 서비스 데이터베이스 스키마
-- =====================================================

-- 데이터베이스 생성 (Docker Compose에서는 자동 생성됨)
-- CREATE DATABASE IF NOT EXISTS wirebarley
--     DEFAULT CHARACTER SET utf8mb4
--     DEFAULT COLLATE utf8mb4_unicode_ci;

-- USE wirebarley;

-- =====================================================
-- 1. accounts (계좌) 테이블
-- =====================================================
-- 계좌 정보를 저장하는 테이블
-- - 계좌번호는 고유해야 함
-- - 잔액은 DECIMAL로 정확한 금액 계산
-- - version은 JPA 낙관적 락용

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '계좌 고유 식별자',
    account_number VARCHAR(20) NOT NULL COMMENT '계좌번호',
    account_holder VARCHAR(50) NOT NULL COMMENT '예금주명',
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '현재 잔액',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성일시',
    updated_at DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정일시',

    PRIMARY KEY (id),
    UNIQUE KEY uk_account_number (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='계좌 정보';

-- =====================================================
-- 2. transactions (거래내역) 테이블
-- =====================================================
-- 모든 거래 내역을 저장하는 테이블
-- - type: 입금(DEPOSIT), 출금(WITHDRAWAL), 이체출금(TRANSFER_OUT), 이체입금(TRANSFER_IN)
-- - from_account_id: 출금 계좌 (입금 시 NULL)
-- - to_account_id: 입금 계좌 (출금 시 NULL)
-- - balance_after: 거래 후 잔액 (해당 계좌 기준)

CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '거래 고유 식별자',
    type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_OUT', 'TRANSFER_IN') NOT NULL COMMENT '거래 유형',
    amount DECIMAL(15,2) NOT NULL COMMENT '거래 금액',
    fee DECIMAL(15,2) DEFAULT NULL COMMENT '수수료 (이체 시)',
    from_account_id BIGINT DEFAULT NULL COMMENT '출금 계좌 ID',
    to_account_id BIGINT DEFAULT NULL COMMENT '입금 계좌 ID',
    balance_after DECIMAL(15,2) NOT NULL COMMENT '거래 후 잔액',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '거래 일시',

    PRIMARY KEY (id),
    INDEX idx_from_account (from_account_id),
    INDEX idx_to_account (to_account_id),
    INDEX idx_created_at (created_at),

    CONSTRAINT fk_from_account
        FOREIGN KEY (from_account_id) REFERENCES accounts(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_to_account
        FOREIGN KEY (to_account_id) REFERENCES accounts(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='거래 내역';

-- =====================================================
-- 인덱스 설명
-- =====================================================
-- uk_account_number: 계좌번호 중복 방지 및 빠른 조회
-- idx_from_account: 출금 계좌별 거래 내역 조회
-- idx_to_account: 입금 계좌별 거래 내역 조회
-- idx_created_at: 최신순 정렬 및 기간별 조회
