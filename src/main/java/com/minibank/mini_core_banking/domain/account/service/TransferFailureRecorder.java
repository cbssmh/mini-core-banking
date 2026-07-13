package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.global.RequestIdHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferFailureRecorder {

    private final TransferHistoryRepository transferHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(TransferRequest request, ErrorCode errorCode, String failureReason) {
        LocalDateTime now = LocalDateTime.now();

        try {
            transferHistoryRepository.saveAndFlush(TransferHistory.builder()
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .idempotencyKey(request.getIdempotencyKey())
                    .requestId(RequestIdHolder.getOrCreate())
                    .errorCode(errorCode.name())
                    .failureReason(truncate(failureReason))
                    .status(TransferStatus.FAILED)
                    .transferredAt(now)
                    .completedAt(now)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // Another request with the same idempotency key already recorded the outcome.
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }
}
