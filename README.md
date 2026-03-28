# 🏦 Mini Core Banking System

Spring Boot 기반의 미니 계정계(Core Banking) 프로젝트입니다.  
단순 CRUD 수준이 아니라, **계좌 이체의 정합성, 거래 상태 관리, 예외 처리, 동시성 제어**를 중심으로 설계했습니다.

---

## 🚀 프로젝트 개요

이 프로젝트는 계정계 시스템의 핵심 개념을 직접 구현하며 학습하기 위해 만들었습니다.

주요 설계 목표:

- 계좌 생성 / 조회 / 이체 기능 구현
- `@Transactional` 기반 이체 정합성 보장
- 전역 예외 처리 구조 적용
- 이체 내역 기록 및 조회
- 비즈니스 규칙 검증
- 동시성 문제 인식 및 비관적 락 적용
- 상태 기반 거래 흐름(`PENDING`, `SUCCESS`, `FAILED`) 설계

---

## 🛠 Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA
- MySQL
- Lombok
- Gradle

---

## ✨ 주요 기능

### 계좌 관리
- 계좌 생성
- 계좌 전체 조회
- 계좌 단건 조회
- 계좌번호 중복 검사

### 계좌 이체
- 계좌 간 금액 이체
- `@Transactional` 기반 처리
- 자기 자신에게 이체 금지
- 0원 이하 이체 금지
- 잔액 부족 검증

### 이체 이력 관리
- 전체 이체 내역 조회
- 특정 계좌 기준 이체 내역 조회
- 이체 상태 관리
    - `PENDING`
    - `SUCCESS`
    - `FAILED`

### 예외 처리
- `CustomException`
- `GlobalExceptionHandler`
- 일관된 JSON 에러 응답 반환

### 동시성 대응
- JPA 비관적 락(`PESSIMISTIC_WRITE`) 적용
- 이체 시 동일 계좌 동시 접근으로 인한 잔액 정합성 문제 완화

---

## 🧱 시스템 구조

    Controller → Service → Repository

    Client
      ↓
    Controller (요청/응답 처리)
      ↓
    Service (비즈니스 로직, 트랜잭션)
      ↓
    Repository (DB 접근)
      ↓
    MySQL

---

## 📁 프로젝트 구조

    src/main/java/com/minibank/mini_core_banking
    ├── domain
    │   └── account
    │       ├── controller
    │       ├── dto
    │       ├── exception
    │       ├── history
    │       │   ├── controller
    │       │   ├── repository
    │       │   ├── TransferHistory.java
    │       │   └── TransferStatus.java
    │       ├── repository
    │       ├── service
    │       └── Account.java
    └── global
        └── GlobalExceptionHandler.java

---

## 🔌 API

### Account API
    POST /accounts
    GET /accounts
    GET /accounts/{id}

### Transfer API
    POST /accounts/transfer

### Transfer History API
    GET /transfers
    GET /transfers/account/{accountId}

---

## 💾 데이터베이스

### account
- id
- account_number
- balance
- owner_name

### transfer_history
- id
- from_account_id
- to_account_id
- amount
- transferred_at
- status

---

## ⚙️ 실행 방법

### 1. MySQL에서 DB 생성
    CREATE DATABASE minibank;

### 2. application.yml 설정
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/minibank
        username: root
        password: your_password

### 3. 프로젝트 실행
    ./gradlew bootRun

---

## 🧪 테스트 예시

### 계좌 생성
    POST http://localhost:8080/accounts
    Content-Type: application/json

    {
      "accountNumber": "111-222-333",
      "balance": 100000,
      "ownerName": "kim"
    }

### 계좌 이체
    POST http://localhost:8080/accounts/transfer
    Content-Type: application/json

    {
      "fromAccountId": 1,
      "toAccountId": 2,
      "amount": 5000
    }

### 전체 이체 내역 조회
    GET http://localhost:8080/transfers

---

## 🔥 핵심 설계 포인트

### 1. 트랜잭션 정합성
이체는 출금, 입금, 이력 저장이 하나의 흐름으로 묶이는 작업이므로 `@Transactional`을 적용했습니다.

### 2. 거래 상태 관리
이체를 즉시 완료 처리하지 않고 `PENDING → SUCCESS / FAILED` 상태 흐름으로 관리하는 방향으로 설계했습니다.

### 3. 전역 예외 처리
`CustomException`과 `GlobalExceptionHandler`를 사용해 에러를 일관된 JSON 형태로 반환했습니다.

### 4. 동시성 제어
동일 계좌에 대한 동시 이체 요청으로 인해 잔액 검증이 깨질 수 있는 문제를 고려해 비관적 락(`PESSIMISTIC_WRITE`)을 적용했습니다.

---

## 📌 개선 가능 포인트

- 데드락 방지를 위한 락 순서 통일
- 트랜잭션 분리를 통한 실패 상태 영속화 고도화
- Swagger/OpenAPI 문서화
- 인증/인가(JWT) 추가
- 테스트 코드 작성

---

## 👨‍💻 Author

Backend Developer 지향  
계정계 / 데이터 정합성 / 트랜잭션 / 시스템 설계 중심 학습