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
    private final TransferMetricsRecorder transferMetricsRecorder;

    public TransferResponse transfer(TransferRequest request) {
        validateRequest(request);
        transferMetricsRecorder.recordAttempt();

        long startNanos = System.nanoTime();
        String outcome = "unknown";
        ErrorCode errorCode = null;
        TransferProcessingResult result;
        try {
            result = transferProcessor.process(request);
            if (result.outcome() == TransferOutcome.REPLAY) {
                outcome = "replay";
                transferMetricsRecorder.recordReplay();
            } else if (result.outcome() == TransferOutcome.FAILED) {
                outcome = "failed";
                errorCode = result.errorCode();
                transferMetricsRecorder.recordFailed();
            } else {
                outcome = "success";
                transferMetricsRecorder.recordSuccess();
            }
        } catch (CustomException e) {
            errorCode = e.getErrorCode();
            if (e.getErrorCode() == ErrorCode.IDEMPOTENCY_CONFLICT) {
                outcome = "conflict";
                transferMetricsRecorder.recordConflict();
            } else {
                outcome = "error";
            }
            throw e;
        } finally {
            transferMetricsRecorder.recordDuration(startNanos, outcome, errorCode);
        }
        // process() has returned through the transactional proxy: FAILED is durable.
        if (result.outcome() == TransferOutcome.FAILED) {
            throw new CustomException(result.errorCode(), result.failureReason());
        }
        return result.response();
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

    List<Long> orderedAccountIds(Long firstAccountId, Long secondAccountId) {
        return transferProcessor.orderedAccountIds(firstAccountId, secondAccountId);
    }
}
