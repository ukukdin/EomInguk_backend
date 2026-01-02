-- =====================================================
-- 송금 서비스 테스트용 초기 데이터
-- =====================================================
-- 이 스크립트는 테스트 및 데모 목적으로 사용됩니다.
-- 운영 환경에서는 사용하지 마세요.

-- =====================================================
-- 1. 테스트용 계좌 생성
-- =====================================================

INSERT INTO accounts (account_number, account_holder, balance, version, created_at) VALUES
('1234567890', '홍길동', 1000000.00, 0, NOW()),
('0987654321', '김철수', 500000.00, 0, NOW()),
('1111111111', '이영희', 2000000.00, 0, NOW()),
('2222222222', '박민수', 0.00, 0, NOW());

-- =====================================================
-- 2. 테스트용 거래 내역 생성
-- =====================================================

-- 홍길동 계좌 입금
INSERT INTO transactions (type, amount, fee, from_account_id, to_account_id, balance_after, created_at) VALUES
('DEPOSIT', 1000000.00, NULL, NULL, 1, 1000000.00, NOW() - INTERVAL 5 DAY);

-- 김철수 계좌 입금
INSERT INTO transactions (type, amount, fee, from_account_id, to_account_id, balance_after, created_at) VALUES
('DEPOSIT', 500000.00, NULL, NULL, 2, 500000.00, NOW() - INTERVAL 4 DAY);

-- 이영희 계좌 입금
INSERT INTO transactions (type, amount, fee, from_account_id, to_account_id, balance_after, created_at) VALUES
('DEPOSIT', 2000000.00, NULL, NULL, 3, 2000000.00, NOW() - INTERVAL 3 DAY);

-- 홍길동 -> 김철수 이체 (10만원, 수수료 1000원)
INSERT INTO transactions (type, amount, fee, from_account_id, to_account_id, balance_after, created_at) VALUES
('TRANSFER_OUT', 100000.00, 1000.00, 1, 2, 899000.00, NOW() - INTERVAL 2 DAY),
('TRANSFER_IN', 100000.00, NULL, 1, 2, 600000.00, NOW() - INTERVAL 2 DAY);

-- 김철수 출금 (5만원)
INSERT INTO transactions (type, amount, fee, from_account_id, to_account_id, balance_after, created_at) VALUES
('WITHDRAWAL', 50000.00, NULL, 2, NULL, 550000.00, NOW() - INTERVAL 1 DAY);

-- =====================================================
-- 3. 데이터 확인용 쿼리
-- =====================================================

-- 계좌 목록 확인
-- SELECT * FROM accounts;

-- 거래 내역 확인 (최신순)
-- SELECT t.*,
--        fa.account_number as from_account,
--        ta.account_number as to_account
-- FROM transactions t
-- LEFT JOIN accounts fa ON t.from_account_id = fa.id
-- LEFT JOIN accounts ta ON t.to_account_id = ta.id
-- ORDER BY t.created_at DESC;

-- 특정 계좌의 거래 내역 조회
-- SELECT * FROM transactions
-- WHERE from_account_id = 1 OR to_account_id = 1
-- ORDER BY created_at DESC;

-- =====================================================
-- 4. 테스트 시나리오 안내
-- =====================================================

-- [시나리오 1] 입금 테스트
-- POST /api/transactions/deposit
-- { "accountNumber": "1234567890", "amount": 50000 }

-- [시나리오 2] 출금 테스트
-- POST /api/transactions/withdraw
-- { "accountNumber": "1234567890", "amount": 30000 }

-- [시나리오 3] 이체 테스트
-- POST /api/transactions/transfer
-- { "fromAccountNumber": "1234567890", "toAccountNumber": "0987654321", "amount": 10000 }

-- [시나리오 4] 거래내역 조회
-- GET /api/transactions/account/number/1234567890

-- [시나리오 5] 일일 한도 테스트 (출금 100만원 한도)
-- 100만원 이상 출금 시도하면 에러 발생

-- [시나리오 6] 일일 한도 테스트 (이체 300만원 한도)
-- 300만원 이상 이체 시도하면 에러 발생
