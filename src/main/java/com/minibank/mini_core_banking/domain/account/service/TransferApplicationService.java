package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferApplicationService {

    private final TransferProcessor transferProcessor;
    private final TransferFailureRecorder transferFailureRecorder;

    public TransferResponse transfer(TransferRequest request) {
        validateRequest(request);

        try {
            return transferProcessor.process(request);
        } catch (CustomException e) {
            if (shouldRecordFailure(e)) {
                transferFailureRecorder.recordFailure(request, e.getErrorCode(), e.getMessage());
            }
            throw e;
        }
    }

    private void validateRequest(TransferRequest request) {
        if (request.getFromAccountId() == null || request.getToAccountId() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "계좌 ID는 필수입니다.");
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이체 금액은 0보다 커야 합니다.");
        }

        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Idempotency key is required");
        }

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new CustomException(ErrorCode.SELF_TRANSFER);
        }
    }

    private boolean shouldRecordFailure(CustomException e) {
        return e.getErrorCode() == ErrorCode.ACCOUNT_NOT_FOUND
                || e.getErrorCode() == ErrorCode.INSUFFICIENT_BALANCE
                || e.getErrorCode() == ErrorCode.TRANSFER_FAILED;
    }

    List<Long> orderedAccountIds(Long firstAccountId, Long secondAccountId) {
        return transferProcessor.orderedAccountIds(firstAccountId, secondAccountId);
    }
}
