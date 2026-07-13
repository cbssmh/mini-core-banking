package com.minibank.mini_core_banking.domain.account.history.repository;

import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {

    Optional<TransferHistory> findByIdempotencyKey(String idempotencyKey);

    List<TransferHistory> findAllByOrderByIdDesc();

    List<TransferHistory> findByFromAccountIdOrToAccountIdOrderByIdDesc(Long fromAccountId, Long toAccountId);

    @Query(value = "select pg_advisory_xact_lock(hashtext(:idempotencyKey))", nativeQuery = true)
    void acquireIdempotencyLock(@Param("idempotencyKey") String idempotencyKey);
}
