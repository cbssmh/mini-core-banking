package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Caller contract only; PostgreSQL integration tests establish commit/locking behavior. */
class TransferApplicationServiceTest {
    final TransferProcessor processor = mock(TransferProcessor.class);
    final TransferMetricsRecorder metrics = mock(TransferMetricsRecorder.class);
    final TransferApplicationService service = new TransferApplicationService(processor, metrics);

    @Test
    void committedFailedResultBecomesOriginalBusinessErrorAndFailedMetric() {
        TransferRequest request = request();
        when(processor.process(request)).thenReturn(new TransferProcessingResult(
                null, TransferOutcome.FAILED, ErrorCode.INSUFFICIENT_BALANCE, "insufficient"));
        assertThatThrownBy(() -> service.transfer(request))
                .isInstanceOfSatisfying(CustomException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                    assertThat(e.getMessage()).isEqualTo("insufficient");
                });
        InOrder order = inOrder(processor, metrics);
        order.verify(metrics).recordAttempt();
        order.verify(processor).process(request);
        order.verify(metrics).recordFailed();
        order.verify(metrics).recordDuration(anyLong(), eq("failed"), eq(ErrorCode.INSUFFICIENT_BALANCE));
        verify(metrics, never()).recordSuccess();
    }

    @Test
    void persistenceFailureEscapesWithoutClaimingDurableBusinessFailure() {
        TransferRequest request = request();
        RuntimeException failure = new DataAccessResourceFailureException("database unavailable");
        when(processor.process(request)).thenThrow(failure);
        assertThatThrownBy(() -> service.transfer(request)).isSameAs(failure);
        verify(metrics, never()).recordFailed();
        verify(metrics, never()).recordSuccess();
    }

    private TransferRequest request() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(200L);
        request.setIdempotencyKey("failure-contract");
        return request;
    }
}
