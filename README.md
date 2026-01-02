# 송금 서비스 API

계좌 간 송금을 처리하는 백엔드 서비스입니다.

---

## 목차

1. [주요 기능](#주요-기능)
2. [실행 방법](#실행-방법)
3. [API 사용법](#api-사용법)
4. [기술 스택](#기술-스택)
5. [프로젝트 구조](#프로젝트-구조)

---

## 주요 기능

### 계좌 관리
- 새로운 계좌를 등록할 수 있습니다.
- 잔액이 0원인 계좌를 삭제할 수 있습니다.
- 계좌 정보를 조회할 수 있습니다.

### 입금
- 원하는 계좌에 금액을 입금할 수 있습니다.

### 출금
- 계좌에서 금액을 출금할 수 있습니다.
- 하루 최대 1,000,000원까지 출금 가능합니다.

### 이체
- 한 계좌에서 다른 계좌로 금액을 보낼 수 있습니다.
- 이체 금액의 1%가 수수료로 부과됩니다.
- 하루 최대 3,000,000원까지 이체 가능합니다.

### 거래내역 조회
- 특정 계좌의 모든 거래내역을 확인할 수 있습니다.
- 가장 최근 거래부터 순서대로 보여줍니다.

---

## 실행 방법

#### 1단계: 프로젝트 다운로드

방법 A - Git 사용:
```bash
git clone https://github.com/ukukdin/wirebarley-backend-assignment.git
cd wirebarley-backend-assignment
```
#### 2단계: 서비스 시작

터미널(명령 프롬프트)에서 다음 명령어를 입력합니다:

```bash
docker-compose up --build
```

처음 실행 시 필요한 파일을 다운로드하므로 5~10분 정도 소요될 수 있습니다.

아래와 같은 메시지가 나타나면 정상적으로 실행된 것입니다:
```
wirebarley-app  | Started WirebarleyApplication in X.XXX seconds
```

#### 3단계: 서비스 사용

웹 브라우저를 열고 아래 주소로 접속합니다:

- API 문서 화면: http://localhost:8080/swagger-ui.html

이 화면에서 모든 API를 직접 테스트해볼 수 있습니다.

#### 서비스 종료

터미널에서 `Ctrl + C`를 누르거나, 새 터미널에서 다음 명령어를 실행합니다:

```bash
docker-compose down
```

데이터베이스 데이터까지 모두 삭제하려면:
```bash
docker-compose down -v
```

---

## API 사용법

### 기본 주소

모든 API는 `http://localhost:8080` 으로 시작합니다.

### 계좌 API

| 기능 | 방식 | 주소 |
|------|------|------|
| 계좌 등록 | POST | /api/accounts |
| 계좌 삭제 | DELETE | /api/accounts/{계좌ID} |
| 계좌 조회 (ID) | GET | /api/accounts/{계좌ID} |
| 계좌 조회 (계좌번호) | GET | /api/accounts/number/{계좌번호} |
| 전체 계좌 목록 | GET | /api/accounts |

#### 계좌 등록 예시

요청:
```json
{
  "accountNumber": "1234-5678-9012",
  "accountHolder": "홍길동"
}
```

응답:
```json
{
  "id": 1,
  "accountNumber": "1234-5678-9012",
  "accountHolder": "홍길동",
  "balance": 0
}
```

### 거래 API

| 기능 | 방식 | 주소 |
|------|------|------|
| 입금 | POST | /api/transactions/deposit |
| 출금 | POST | /api/transactions/withdraw |
| 이체 | POST | /api/transactions/transfer |
| 거래내역 조회 (ID) | GET | /api/transactions/account/{계좌ID} |
| 거래내역 조회 (계좌번호) | GET | /api/transactions/account/number/{계좌번호} |

#### 입금 예시

요청:
```json
{
  "accountNumber": "1234-5678-9012",
  "amount": 100000
}
```

응답:
```json
{
  "id": 1,
  "type": "DEPOSIT",
  "amount": 100000,
  "fee": 0,
  "balanceAfter": 100000,
  "createdAt": "2024-01-02T10:30:00"
}
```

#### 출금 예시

요청:
```json
{
  "accountNumber": "1234-5678-9012",
  "amount": 50000
}
```

#### 이체 예시

요청:
```json
{
  "fromAccountNumber": "1234-5678-9012",
  "toAccountNumber": "9876-5432-1098",
  "amount": 10000
}
```

응답 (수수료 1% = 100원 차감):
```json
{
  "id": 3,
  "type": "TRANSFER_OUT",
  "amount": 10000,
  "fee": 100,
  "balanceAfter": 39900,
  "createdAt": "2024-01-02T10:35:00"
}
```

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.5 |
| 데이터베이스 | MySQL 8.0 |
| ORM | Spring Data JPA (Hibernate) |
| API 문서 | SpringDoc OpenAPI (Swagger) |
| 빌드 도구 | Gradle |
| 컨테이너 | Docker, Docker Compose |

---

## 프로젝트 구조

```
wirebarley-backend-assignment/
│
├── common-module/          # 공통 코드 (예외 처리, 설정 등)
├── account-module/         # 계좌 관련 기능
├── transaction-module/     # 거래 관련 기능 (입금, 출금, 이체)
├── application/            # 애플리케이션 실행 모듈
│
├── docker-compose.yml      # Docker 실행 설정
├── Dockerfile              # 애플리케이션 빌드 설정
└── README.md               # 현재 문서
```

### 모듈별 역할

**common-module**
- 모든 모듈에서 공통으로 사용하는 코드
- 에러 처리, 응답 형식 등

**account-module**
- 계좌 등록, 삭제, 조회 기능
- 계좌 정보 관리

**transaction-module**
- 입금, 출금, 이체 기능
- 거래내역 저장 및 조회
- 일일 한도 검증

**application**
- 전체 애플리케이션 실행
- 설정 파일 관리

---

## 주의사항

### 한도 제한
- 출금: 1일 최대 1,000,000원
- 이체: 1일 최대 3,000,000원
- 한도는 매일 자정에 초기화됩니다.

### 수수료
- 이체 시 이체 금액의 1%가 수수료로 부과됩니다.
- 수수료는 보내는 사람의 계좌에서 차감됩니다.
- 예: 10,000원 이체 시 총 10,100원이 차감됩니다.

### 계좌 삭제
- 잔액이 남아있는 계좌는 삭제할 수 없습니다.
- 삭제 전 잔액을 모두 출금하거나 이체해야 합니다.

---
.
