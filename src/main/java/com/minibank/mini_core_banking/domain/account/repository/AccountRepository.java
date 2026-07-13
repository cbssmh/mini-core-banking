package com.minibank.mini_core_banking.domain.account.repository;

import com.minibank.mini_core_banking.domain.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    default Optional<Account> findByIdForUpdate(Long id) {
        return findById(id);
    }
}
