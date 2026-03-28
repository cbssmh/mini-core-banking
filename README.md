# 🏦 Mini Core Banking System

Spring Boot 기반의 **미니 계정계(Core Banking) 시스템**입니다.  
단순 CRUD가 아니라, **계좌 이체 트랜잭션 정합성과 비즈니스 규칙 처리**에 초점을 맞춰 설계했습니다.

---

## 🚀 프로젝트 개요

이 프로젝트는 다음을 목표로 합니다:

- 계정계 시스템의 핵심 개념 이해 (정합성, 트랜잭션)
- 단순 기능 구현이 아닌 **비즈니스 로직 중심 설계**
- **Controller → Service → Repository 구조 분리**
- 금융 시스템에서 중요한 **데이터 무결성 보장**

---

## 🛠 Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Data JPA**
- **MySQL**
- **Lombok**
- **Gradle**

---

## ✨ 주요 기능

### 1. 계좌 관리
- 계좌 생성
- 계좌 전체 조회
- 계좌 단건 조회
- 계좌번호 중복 검사

### 2. 계좌 이체
- 계좌 간 금액 이체
- 트랜잭션 기반 처리 (`@Transactional`)
- 이체 성공 시 이력 저장

### 3. 이체 이력 관리
- 전체 이체 내역 조회
- 특정 계좌 기준 이체 내역 조회

---

## ⚠️ 비즈니스 규칙

```text
- 자기 자신에게 이체 불가
- 이체 금액은 0보다 커야 함
- 잔액 부족 시 이체 실패
- 존재하지 않는 계좌 접근 차단
- 중복 계좌번호 생성 방지
```

---

## 🧱 시스템 구조

```text
Controller → Service → Repository

Client
  ↓
Controller (요청 처리)
  ↓
Service (비즈니스 로직 + 트랜잭션)
  ↓
Repository (DB 접근)
  ↓
Database
```

---

## 📁 프로젝트 구조

```text
src/main/java/com/minibank/mini_core_banking
├── domain
│   └── account
│       ├── controller
│       ├── dto
│       ├── exception
│       ├── history
│       │   ├── controller
│       │   └── repository
│       ├── repository
│       ├── service
│       └── Account.java
└── global
    └── GlobalExceptionHandler.java
```

---

## 🔌 API 명세

### 📌 Account API

| Method | URL            | 설명            |
|--------|----------------|-----------------|
| POST   | /accounts      | 계좌 생성        |
| GET    | /accounts      | 전체 계좌 조회   |
| GET    | /accounts/{id} | 계좌 단건 조회   |

---

### 💸 Transfer API

| Method | URL                | 설명        |
|--------|--------------------|-------------|
| POST   | /accounts/transfer | 계좌 이체    |

---

### 📊 Transfer History API

| Method | URL                           | 설명                |
|--------|--------------------------------|---------------------|
| GET    | /transfers                     | 전체 이체 내역 조회 |
| GET    | /transfers/account/{accountId} | 계좌별 이체 내역 조회 |

---

## 🧪 테스트 방법

test.http 파일을 이용해 API 테스트 가능

### 계좌 생성

```http
POST http://localhost:8080/accounts
Content-Type: application/json

{
  "accountNumber": "111-222-333",
  "balance": 10000,
  "ownerName": "kim"
}
```

### 이체

```http
POST http://localhost:8080/accounts/transfer
Content-Type: application/json

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 5000
}
```

---

## 💾 데이터베이스

### account

| 필드           | 설명       |
|----------------|------------|
| id             | PK         |
| account_number | 계좌번호   |
| balance        | 잔액       |
| owner_name     | 예금주     |

---

### transfer_history

| 필드             | 설명       |
|------------------|------------|
| id               | PK         |
| from_account_id  | 출금 계좌  |
| to_account_id    | 입금 계좌  |
| amount           | 금액       |
| transferred_at   | 이체 시각  |

---

## ⚙️ 실행 방법

### 1. MySQL에서 DB 생성

```sql
CREATE DATABASE minibank;
```

### 2. application.yml 설정

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/minibank
    username: root
    password: your_password
```

### 3. 실행

```bash
./gradlew bootRun
```

---

## 🔥 핵심 설계 포인트

### 1. 트랜잭션 정합성 보장

```java
@Transactional
public void transfer(...) {
    // 출금 + 입금 + 이력 저장
}
```

👉 출금 + 입금 + 이력 저장  
→ 하나라도 실패하면 전체 롤백

---

### 2. 예외 처리 구조

```text
CustomException → GlobalExceptionHandler
```

👉 모든 예외를 일관된 JSON 형태로 반환

---

### 3. 계층 분리

- Controller: 요청 처리
- Service: 비즈니스 로직
- Repository: DB 접근

👉 유지보수성과 확장성 확보

---

## 📌 개선 가능 포인트

- 동시성 문제 해결 (락 / 버전 관리)
- Swagger API 문서화
- JWT 인증 추가
- MSA 구조 확장

---

## 👨‍💻 Author

Backend Developer 지향  
계정계 / 데이터 정합성 / 시스템 설계 중심 학습