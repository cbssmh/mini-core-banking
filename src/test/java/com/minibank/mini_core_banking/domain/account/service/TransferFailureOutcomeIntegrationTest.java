package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import com.minibank.mini_core_banking.global.RequestIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;
import java.util.concurrent.atomic.AtomicInteger;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Forces a second request to wait on the first request's uncommitted FAILED outcome. */
@SpringBootTest
@Testcontainers
class TransferFailureOutcomeIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void dataSource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired TransferApplicationService transfers;
    @Autowired AccountRepository accounts;
    @Autowired TransferHistoryRepository histories;
    @MockitoSpyBean TransferProcessor processor;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        histories.deleteAllInBatch();
        accounts.deleteAllInBatch();
    }

    @AfterEach
    void clearRequestId() { RequestIdHolder.clear(); }

    @Test
    void concurrentSamePayloadReplaysFailedOutcomeWithoutUnexpectedRollback() throws Exception {
        contendOnFailedOutcome(false);
    }

    @Test
    void concurrentDifferentPayloadCannotClaimFailedOutcomesKey() throws Exception {
        contendOnFailedOutcome(true);
    }

    private void contendOnFailedOutcome(boolean differentPayload) throws Exception {
        Account a = accounts.save(Account.builder().accountNumber("race-a").ownerName("A").balance(100L).build());
        Account b = accounts.save(Account.builder().accountNumber("race-b").ownerName("B").balance(0L).build());
        CountDownLatch beforeCommit = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        AtomicInteger holderPid = new AtomicInteger();
        doAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .as("Spy must execute inside the processor's Spring transaction").isTrue();
            TransferProcessingResult result = (TransferProcessingResult) call.callRealMethod();
            if (result.outcome() == TransferOutcome.FAILED) {
                holderPid.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        beforeCommit.countDown();
                        try {
                            assertThat(releaseCommit.await(15, TimeUnit.SECONDS))
                                    .as("Release the FAILED transaction after observing its contender").isTrue();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError("Interrupted at FAILED commit gate", e);
                        }
                    }
                });
            }
            return result;
        }).when(processor).process(any());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(() -> transferOutcome(request(a.getId(), b.getId(), 200L)));
            assertThat(beforeCommit.await(10, TimeUnit.SECONDS)).as("First request reaches commit gate").isTrue();
            Future<Object> second = executor.submit(() -> transferOutcome(
                    request(a.getId(), b.getId(), differentPayload ? 50L : 200L)));
            awaitAdvisoryWait(holderPid.get(), second);
            releaseCommit.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isInstanceOfSatisfying(CustomException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE));
            Object secondOutcome = second.get(10, TimeUnit.SECONDS);
            if (differentPayload) {
                assertThat(secondOutcome).isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
            } else {
                assertThat(secondOutcome).isInstanceOfSatisfying(TransferResponse.class,
                        response -> assertThat(response.getStatus()).isEqualTo(TransferStatus.FAILED));
            }
            assertThat(accounts.findById(a.getId()).orElseThrow().getBalance()).isEqualTo(100L);
            assertThat(accounts.findById(b.getId()).orElseThrow().getBalance()).isZero();
            assertThat(histories.findAll()).singleElement().satisfies(h -> {
                assertThat(h.getStatus()).isEqualTo(TransferStatus.FAILED);
                assertThat(h.getAmount()).isEqualTo(200L);
                assertThat(h.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE.name());
                assertThat(h.getCompletedAt()).isNotNull();
            });
        } finally {
            releaseCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Object transferOutcome(TransferRequest request) {
        try {
            return transfers.transfer(request);
        } catch (CustomException expectedBusinessOutcome) {
            return expectedBusinessOutcome;
        } finally {
            RequestIdHolder.clear();
        }
    }

    private void awaitAdvisoryWait(int holderPid, Future<?> contender) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        // No surrounding observer transaction: each query gets a fresh activity snapshot.
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("""
                    select exists(select 1 from pg_stat_activity
                    where wait_event_type = 'Lock' and wait_event = 'advisory'
                    and ? = any(pg_blocking_pids(pid)))
                    """, Boolean.class, holderPid))) return;
            if (contender.isDone()) {
                fail("Contender completed before waiting on FAILED key: " + contender.get());
            }
            Thread.sleep(20);
        }
        fail("No contender advisory wait observed; backends=" + jdbc.queryForList(
                "select pid, state, wait_event_type, wait_event, query, pg_blocking_pids(pid) from pg_stat_activity"));
    }

    private TransferRequest request(long from, long to, long amount) {
        TransferRequest r = new TransferRequest();
        r.setFromAccountId(from); r.setToAccountId(to); r.setAmount(amount);
        r.setIdempotencyKey("failure-window");
        return r;
    }
}
