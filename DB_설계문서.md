# 데이터베이스 설계 문서

## 1. 개요

송금 서비스의 데이터베이스 설계 문서입니다. 계좌 관리와 거래 내역 저장을 위한 테이블 구조를 정의합니다.

---

## 2. ERD (Entity Relationship Diagram)

```
+------------------+          +--------------------+
|     accounts     |          |    transactions    |
+------------------+          +--------------------+
| PK id            |<─────────| PK id              |
|    account_number|    ┌────>| FK from_account_id |
|    account_holder|    │ ┌──>| FK to_account_id   |
|    balance       |────┘ │   |    type            |
|    version       |──────┘   |    amount          |
|    created_at    |          |    fee             |
|    updated_at    |          |    balance_after   |
+------------------+          |    created_at      |
                              +--------------------+
```

**관계 설명:**
- accounts : transactions = 1 : N (하나의 계좌는 여러 거래를 가질 수 있음)
- 거래(Transaction)는 출금 계좌(from_account)와 입금 계좌(to_account)를 참조
- 입금: to_account만 존재
- 출금: from_account만 존재
- 이체: from_account와 to_account 모두 존재

---

## 3. 테이블 명세

### 3.1 accounts (계좌)

계좌 정보를 저장하는 테이블입니다.

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 계좌 고유 식별자 |
| account_number | VARCHAR(20) | NOT NULL, UNIQUE | 계좌번호 |
| account_holder | VARCHAR(50) | NOT NULL | 예금주명 |
| balance | DECIMAL(15,2) | NOT NULL, DEFAULT 0 | 현재 잔액 |
| version | BIGINT | - | 낙관적 락을 위한 버전 |
| created_at | DATETIME(6) | NOT NULL | 계좌 생성일시 |
| updated_at | DATETIME(6) | - | 최종 수정일시 |

**인덱스:**

| 인덱스명 | 컬럼 | 유형 | 용도 |
|---------|------|------|------|
| PRIMARY | id | PK | 기본키 |
| UK_account_number | account_number | UNIQUE | 계좌번호 중복 방지 및 조회 |

**설계 근거:**

1. **account_number (VARCHAR 20)**
   - 일반적인 계좌번호 형식(숫자+하이픈)을 수용
   - UNIQUE 제약으로 중복 계좌번호 방지

2. **balance (DECIMAL 15,2)**
   - 금융 데이터는 부동소수점 오차 방지를 위해 DECIMAL 사용
   - 최대 9,999,999,999,999.99원까지 표현 가능
   - 소수점 2자리로 원 단위 정확한 계산

3. **version**
   - JPA @Version 어노테이션으로 낙관적 락 구현
   - 동시 수정 시 충돌 감지용

4. **created_at, updated_at**
   - JPA Auditing으로 자동 관리
   - 데이터 변경 이력 추적

### 3.2 transactions (거래내역)

모든 거래 내역을 저장하는 테이블입니다.

| 컬럼명 | 데이터 타입 | 제약조건 | 설명 |
|--------|------------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 거래 고유 식별자 |
| type | ENUM | NOT NULL | 거래 유형 |
| amount | DECIMAL(15,2) | NOT NULL | 거래 금액 |
| fee | DECIMAL(15,2) | NULLABLE | 수수료 (이체 시) |
| from_account_id | BIGINT | FK, NULLABLE | 출금 계좌 ID |
| to_account_id | BIGINT | FK, NULLABLE | 입금 계좌 ID |
| balance_after | DECIMAL(15,2) | NOT NULL | 거래 후 잔액 |
| created_at | DATETIME(6) | NOT NULL | 거래 일시 |

**거래 유형 (type):**

| 값 | 설명 | from_account | to_account |
|----|------|--------------|------------|
| DEPOSIT | 입금 | NULL | 입금 계좌 |
| WITHDRAWAL | 출금 | 출금 계좌 | NULL |
| TRANSFER_OUT | 이체 출금 | 보내는 계좌 | 받는 계좌 |
| TRANSFER_IN | 이체 입금 | 보내는 계좌 | 받는 계좌 |

**인덱스:**

| 인덱스명 | 컬럼 | 유형 | 용도 |
|---------|------|------|------|
| PRIMARY | id | PK | 기본키 |
| idx_transaction_from_account | from_account_id | INDEX | 출금 계좌별 거래 조회 |
| idx_transaction_to_account | to_account_id | INDEX | 입금 계좌별 거래 조회 |
| idx_transaction_created_at | created_at | INDEX | 기간별 거래 조회, 정렬 |

**설계 근거:**

1. **type (ENUM)**
   - 거래 유형을 명확하게 구분
   - 이체의 경우 출금/입금 각각 별도 레코드로 기록하여 각 계좌의 거래 내역 추적 용이

2. **from_account_id, to_account_id (NULLABLE FK)**
   - 입금: to_account_id만 존재
   - 출금: from_account_id만 존재
   - 이체: 양쪽 모두 존재
   - NULLABLE로 설계하여 거래 유형에 따른 유연한 처리

3. **balance_after**
   - 거래 시점의 잔액을 기록
   - 거래 내역 조회 시 별도 계산 없이 바로 표시 가능
   - 잔액 변동 추적 및 감사(audit) 용이

4. **fee**
   - 이체 시 발생하는 수수료 기록
   - 입금/출금은 수수료가 없으므로 NULL

5. **인덱스 설계**
   - from_account_id, to_account_id: 계좌별 거래 내역 조회 성능
   - created_at: 최신순 정렬 및 기간 검색 성능

---

## 4. 관계 설명

### 4.1 Account - Transaction 관계

```
Account (1) ──────< Transaction (N)
         from_account_id
         to_account_id
```

- 하나의 계좌는 여러 거래를 가질 수 있음 (1:N)
- Transaction은 Account를 두 번 참조 (from, to)
- 양방향 참조는 불필요하여 단방향으로 설계 (Transaction -> Account)

### 4.2 거래 유형별 FK 사용

| 거래 유형 | from_account_id | to_account_id | 설명 |
|----------|-----------------|---------------|------|
| DEPOSIT | NULL | O | 입금 계좌만 기록 |
| WITHDRAWAL | O | NULL | 출금 계좌만 기록 |
| TRANSFER_OUT | O | O | 보내는 계좌 기준 기록 |
| TRANSFER_IN | O | O | 받는 계좌 기준 기록 |

**이체 시 두 개의 레코드가 생성되는 이유:**
- 각 계좌 관점에서 거래 내역을 조회할 때 편리
- 보내는 사람: TRANSFER_OUT으로 조회
- 받는 사람: TRANSFER_IN으로 조회
- balance_after가 각 계좌별로 정확하게 기록됨

---

## 5. 데이터 타입 선정 근거

### 5.1 금액 관련 필드 - DECIMAL(15,2)

| 고려사항 | DECIMAL | DOUBLE/FLOAT |
|---------|---------|--------------|
| 정확도 | 정확한 십진수 연산 | 부동소수점 오차 발생 가능 |
| 금융 적합성 | 적합 | 부적합 |
| 저장 공간 | 다소 큼 | 작음 |

**선택: DECIMAL(15,2)**
- 금융 데이터는 정확한 계산이 필수
- 0.1 + 0.2 = 0.30000000000000004 같은 오차 방지
- 최대 13자리 정수 + 2자리 소수 = 약 10조원까지 표현

### 5.2 계좌번호 - VARCHAR(20)

```
예시 형식:
- 1234567890 (숫자만)
- 123-456-789012 (하이픈 포함)
- 1234-5678-9012 (하이픈 포함)
```

- 다양한 형식의 계좌번호 수용
- CHAR 대신 VARCHAR로 가변 길이 지원

### 5.3 거래 유형 - ENUM

```sql
ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_OUT', 'TRANSFER_IN')
```

- 정해진 값만 허용하여 데이터 무결성 보장
- 문자열 저장보다 효율적인 저장 공간
- 코드와 DB 간 타입 안전성

### 5.4 일시 - DATETIME(6)

- 마이크로초 단위까지 저장 (6자리)
- 동시 거래 시 정확한 순서 구분
- 시간대는 서버 기준 (Asia/Seoul)

---

## 6. 성능 고려사항

### 6.1 인덱스 전략

**accounts 테이블:**
```sql
-- 계좌번호 조회 (가장 빈번한 조회)
CREATE UNIQUE INDEX UK_account_number ON accounts(account_number);
```

**transactions 테이블:**
```sql
-- 계좌별 거래 내역 조회
CREATE INDEX idx_transaction_from_account ON transactions(from_account_id);
CREATE INDEX idx_transaction_to_account ON transactions(to_account_id);

-- 최신순 정렬 및 기간 검색
CREATE INDEX idx_transaction_created_at ON transactions(created_at);
```

### 6.2 조회 최적화

**거래 내역 조회 쿼리:**
```sql
-- 특정 계좌의 거래 내역 (최신순)
SELECT * FROM transactions
WHERE from_account_id = ? OR to_account_id = ?
ORDER BY created_at DESC
LIMIT 20;
```

**복합 인덱스 고려:**
- 현재 단일 컬럼 인덱스로 충분한 성능
- 향후 조회 패턴에 따라 복합 인덱스 추가 검토

### 6.3 대용량 데이터 대응

**파티셔닝 고려:**
- transactions 테이블은 지속적으로 증가
- 월별/년별 파티셔닝으로 조회 성능 유지 가능
- 현재는 미적용 (MVP 단계)

**아카이빙 전략:**
- 오래된 거래 내역은 별도 아카이브 테이블로 이관
- 실시간 조회 테이블 크기 관리

---

## 7. DDL 스크립트

```sql
-- 계좌 테이블
CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20) NOT NULL,
    account_holder VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    version BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY UK_account_number (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 거래 내역 테이블
CREATE TABLE transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type ENUM('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_OUT', 'TRANSFER_IN') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    fee DECIMAL(15,2),
    from_account_id BIGINT,
    to_account_id BIGINT,
    balance_after DECIMAL(15,2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_transaction_from_account (from_account_id),
    INDEX idx_transaction_to_account (to_account_id),
    INDEX idx_transaction_created_at (created_at),
    FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    FOREIGN KEY (to_account_id) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 8. 확장성 고려

### 8.1 현재 미구현 (향후 확장 가능)

| 기능 | 필요 변경 | 설명 |
|------|----------|------|
| 다중 통화 | currency 컬럼 추가 | 원화 외 다른 통화 지원 |
| 계좌 유형 | account_type 컬럼 추가 | 예금/적금/청약 등 |
| 거래 메모 | memo 컬럼 추가 | 거래 시 메모 기록 |
| 거래 취소 | status, canceled_at 추가 | 거래 취소 기능 |

### 8.2 설계 원칙

- 현재 요구사항에 충실한 최소 설계
- 불필요한 컬럼 미리 추가하지 않음
- 확장 필요 시 마이그레이션으로 대응
