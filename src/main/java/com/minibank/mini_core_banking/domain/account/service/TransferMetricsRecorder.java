package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TransferMetricsRecorder {

    private final Counter attempts;
    private final Counter success;
    private final Counter failed;
    private final Counter replay;
    private final Counter conflict;
    private final MeterRegistry meterRegistry;

    public TransferMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.attempts = Counter.builder("bank.transfer.attempts")
                .description("Valid transfer application requests")
                .register(meterRegistry);
        this.success = Counter.builder("bank.transfer.success")
                .description("New transfers committed successfully")
                .register(meterRegistry);
        this.failed = Counter.builder("bank.transfer.failed")
                .description("Business transfer attempts recorded as failed")
                .register(meterRegistry);
        this.replay = Counter.builder("bank.transfer.idempotency.replay")
                .description("Idempotency requests returning an existing result")
                .register(meterRegistry);
        this.conflict = Counter.builder("bank.transfer.idempotency.conflict")
                .description("Idempotency key conflicts")
                .register(meterRegistry);
    }

    public void recordAttempt() {
        attempts.increment();
    }

    public void recordSuccess() {
        success.increment();
    }

    public void recordFailed() {
        failed.increment();
    }

    public void recordReplay() {
        replay.increment();
    }

    public void recordConflict() {
        conflict.increment();
    }

    public void recordDuration(long startNanos, String outcome, ErrorCode errorCode) {
        Timer.builder("bank.transfer.duration")
                .description("Transfer application service duration")
                .tag("outcome", outcome)
                .tag("error_code", errorCode == null ? "NONE" : errorCode.name())
                .register(meterRegistry)
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
    }
}
