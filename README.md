# 🏦 Mini Core Banking System

A Spring Boot–based mini core banking system designed to go beyond simple CRUD operations.  
This project focuses on transactional consistency, state management, exception handling, and concurrency control in financial operations.

---

## 🚀 Overview

This project was built to implement and deeply understand the core concepts of a banking system.

### Key Objectives
- Implement account creation, retrieval, and transfer features  
- Ensure transactional consistency using `@Transactional`  
- Apply a global exception handling mechanism  
- Record and query transfer histories  
- Enforce business rules and validations  
- Handle concurrency with pessimistic locking  
- Design a state-driven transaction flow (`PENDING`, `SUCCESS`, `FAILED`)  

---

## 🛠 Tech Stack

- Java 21  
- Spring Boot  
- Spring Data JPA  
- MySQL  
- Lombok  
- Gradle  

---

## ✨ Features

### Account Management
- Create account  
- Retrieve all accounts  
- Retrieve account by ID  
- Validate duplicate account numbers  

### Money Transfer
- Transfer funds between accounts  
- Transactional processing with `@Transactional`  
- Prevent self-transfer  
- Reject zero or negative amounts  
- Validate sufficient balance  

### Transfer History
- Retrieve all transfer records  
- Retrieve transfer history by account  
- Manage transfer states:  
  - `PENDING`  
  - `SUCCESS`  
  - `FAILED`  

### Exception Handling
- Custom exception (`CustomException`)  
- Centralized error handling (`GlobalExceptionHandler`)  
- Consistent JSON error responses  

### Concurrency Control
- Applied JPA pessimistic locking (`PESSIMISTIC_WRITE`)  
- Mitigates balance inconsistency under concurrent access  

---

## 🧱 Architecture

```
Controller → Service → Repository

Client
  ↓
Controller (Request/Response handling)
  ↓
Service (Business logic & transactions)
  ↓
Repository (Database access)
  ↓
MySQL
```

---

## 📁 Project Structure

```
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
```

---

## 🔌 API

### Account API
```
POST /accounts
GET /accounts
GET /accounts/{id}
```

### Transfer API
```
POST /accounts/transfer
```

### Transfer History API
```
GET /transfers
GET /transfers/account/{accountId}
```

---

## 💾 Database Schema

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

## ⚙️ Getting Started

### 1. Create Database
```sql
CREATE DATABASE minibank;
```

### 2. Configure application.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/minibank
    username: root
    password: your_password
```

### 3. Run the Application
```bash
./gradlew bootRun
```

---

## 🧪 Example Requests

### Create Account
```http
POST http://localhost:8080/accounts
Content-Type: application/json

{
  "accountNumber": "111-222-333",
  "balance": 100000,
  "ownerName": "kim"
}
```

### Transfer Money
```http
POST http://localhost:8080/accounts/transfer
Content-Type: application/json

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 5000
}
```

### Get Transfer History
```http
GET http://localhost:8080/transfers
```

---

## 🔥 Key Design Decisions

### 1. Transactional Consistency
Transfers involve withdrawal, deposit, and history recording as a single atomic operation, handled using `@Transactional`.

### 2. State-Based Transaction Flow
Instead of treating transfers as instantly completed, the system models them with explicit states:  
`PENDING → SUCCESS / FAILED`

### 3. Centralized Exception Handling
Errors are handled through `CustomException` and `GlobalExceptionHandler`, ensuring consistent JSON responses.

### 4. Concurrency Control
To prevent race conditions on balance updates, pessimistic locking (`PESSIMISTIC_WRITE`) is applied.

---

## 📌 Future Improvements

- Enforce consistent lock ordering to prevent deadlocks  
- Improve failure-state persistence via transaction separation  
- Add Swagger / OpenAPI documentation  
- Implement authentication & authorization (JWT)  
- Write comprehensive test cases  

---

## 👨‍💻 Author

Aspiring Backend Developer  
Focused on core banking systems, data consistency, transactions, and system design