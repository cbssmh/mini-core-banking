package com.minibank.mini_core_banking.domain.account.repository;

import com.minibank.mini_core_banking.domain.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByAccountNumber(String accountNumber);
}