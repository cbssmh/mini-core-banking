package com.minibank.mini_core_banking.domain.account.history.repository;

import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {

    List<TransferHistory> findAllByOrderByIdDesc();

    List<TransferHistory> findByFromAccountIdOrToAccountIdOrderByIdDesc(Long fromAccountId, Long toAccountId);
}