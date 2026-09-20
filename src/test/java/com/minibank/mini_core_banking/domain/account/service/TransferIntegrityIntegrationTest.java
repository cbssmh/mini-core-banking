package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class TransferIntegrityIntegrationTest {
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
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean() {
        histories.deleteAllInBatch();
        accounts.deleteAllInBatch();
    }

    @AfterEach
    void clearRequestId() { RequestIdHolder.clear(); }

    @Test
    void overflowLeavesBalancesUntouchedAndRecordsFailedOutcome() {
        Account a = account(1L);
        Account b = account(Long.MAX_VALUE);
        BigInteger before = total();
        TransferRequest request = request(a, b, 1L, "overflow");
        assertThatThrownBy(() -> transfers.transfer(request)).isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TRANSFER_FAILED);
        assertThat(balance(a)).isEqualTo(1L);
        assertThat(balance(b)).isEqualTo(Long.MAX_VALUE);
        assertThat(total()).isEqualTo(before);
        assertThat(histories.findAll()).singleElement().satisfies(h -> {
            assertThat(h.getStatus()).isEqualTo(TransferStatus.FAILED);
            assertThat(h.getErrorCode()).isEqualTo("TRANSFER_FAILED");
        });
        assertThat(transfers.transfer(request).getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(histories.count()).isEqualTo(1);
    }

    @Test
    void exactMaximumIsValid() {
        Account a = account(Long.MAX_VALUE);
        Account b = account(0L);
        assertThat(transfers.transfer(request(a, b, Long.MAX_VALUE, "max")).getStatus())
                .isEqualTo(TransferStatus.SUCCESS);
        assertThat(balance(a)).isZero();
        assertThat(balance(b)).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void databaseRejectsNegativeBalancesEvenOutsideTransferService() {
        Account a = account(1L);
        assertThatThrownBy(() -> jdbc.update("update account set balance = -1 where id = ?", a.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(balance(a)).isEqualTo(1L);
    }

    @Test
    void competingWithdrawalsCannotOverspend() throws Exception {
        Account a = account(100L);
        Account b = account(0L);
        Account c = account(0L);
        List<Object> outcomes = contend(a,
                request(a, b, 80L, "withdraw-1"), request(a, c, 80L, "withdraw-2"));
        assertThat(outcomes.stream().filter(TransferResponse.class::isInstance).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(CustomException.class::isInstance)
                .map(CustomException.class::cast).map(CustomException::getErrorCode).toList())
                .containsExactly(ErrorCode.INSUFFICIENT_BALANCE);
        assertThat(balance(a)).isEqualTo(20L);
        assertThat(total()).isEqualTo(BigInteger.valueOf(100));
        assertThat(histories.findAll()).extracting(h -> h.getStatus())
                .containsExactlyInAnyOrder(TransferStatus.SUCCESS, TransferStatus.FAILED);
    }

    @Test
    void oppositeDirectionsCompleteWithoutDeadlockUnderForcedContention() throws Exception {
        Account a = account(100L);
        Account b = account(100L);
        List<Object> outcomes = contend(a,
                request(a, b, 30L, "opposite-1"), request(b, a, 20L, "opposite-2"));
        assertThat(outcomes).allSatisfy(o -> {
            assertThat(o).isInstanceOf(TransferResponse.class);
            assertThat(((TransferResponse) o).getStatus()).isEqualTo(TransferStatus.SUCCESS);
        });
        assertThat(balance(a)).isEqualTo(90L);
        assertThat(balance(b)).isEqualTo(110L);
        assertThat(total()).isEqualTo(BigInteger.valueOf(200));
        assertThat(histories.findAll()).extracting(h -> h.getStatus())
                .containsExactly(TransferStatus.SUCCESS, TransferStatus.SUCCESS);
    }

    @Test
    void databaseFailureAtHistoryCompletionRollsBackBalancesAndPendingRow() {
        Account a = account(100L);
        Account b = account(0L);
        // Real server-side error during flush, after managed balances have changed.
        jdbc.execute("create function reject_test_success() returns trigger language plpgsql as $$ "
                + "begin if NEW.status = 'SUCCESS' then raise exception 'injected history failure'; "
                + "end if; return NEW; end $$");
        jdbc.execute("create trigger reject_test_success before update on transfer_history "
                + "for each row execute function reject_test_success()");
        try {
            assertThatThrownBy(() -> transfers.transfer(request(a, b, 30L, "db-failure")))
                    .isInstanceOf(RuntimeException.class).isNotInstanceOf(CustomException.class);
            assertThat(balance(a)).isEqualTo(100L);
            assertThat(balance(b)).isZero();
            assertThat(histories.count()).isZero();
        } finally {
            jdbc.execute("drop trigger reject_test_success on transfer_history");
            jdbc.execute("drop function reject_test_success()");
        }
        // Infrastructure errors have no retained outcome: retry is a fresh attempt.
        assertThat(transfers.transfer(request(a, b, 30L, "db-failure")).getStatus())
                .isEqualTo(TransferStatus.SUCCESS);
    }

    private List<Object> contend(Account locked, TransferRequest first, TransferRequest second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CopyOnWriteArrayList<Future<Object>> futures = new CopyOnWriteArrayList<>();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                accounts.findAllByIdInForUpdate(List.of(locked.getId()));
                futures.add(executor.submit(() -> attempt(first)));
                futures.add(executor.submit(() -> attempt(second)));
                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                List<Map<String, Object>> activity = List.of();
                while (System.nanoTime() < deadline) {
                    // This connection also holds the gate row lock. PostgreSQL caches
                    // activity/query text within a transaction: refresh before EACH poll.
                    // Sleeping longer does not refresh that snapshot.
                    jdbc.execute("select pg_stat_clear_snapshot()");
                    activity = jdbc.queryForList("select pid, state, wait_event_type, wait_event, "
                            + "query, pg_blocking_pids(pid) as blockers from pg_stat_activity "
                            + "where datname = current_database() and pid <> pg_backend_pid()");
                    long waiting = activity.stream().filter(row ->
                            "Lock".equals(row.get("wait_event_type"))
                                    && String.valueOf(row.get("query")).contains("account")).count();
                    if (waiting >= 2) return;
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
                }
                fail("Both workers must reach a PostgreSQL lock wait before release; activity="
                        + activity + "; workers=" + workerDiagnostics(futures));
            });
            return List.of(futures.get(0).get(10, TimeUnit.SECONDS), futures.get(1).get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private List<String> workerDiagnostics(List<Future<Object>> futures) {
        return futures.stream().map(future -> {
            if (!future.isDone()) return "pending";
            try {
                return "completed: " + future.get(0, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                return "failed: " + e.getCause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "observer interrupted";
            } catch (TimeoutException | CancellationException e) {
                return e.toString();
            }
        }).toList();
    }

    private Object attempt(TransferRequest request) {
        try { return transfers.transfer(request); }
        catch (CustomException e) { return e; }
        finally { RequestIdHolder.clear(); }
    }

    private Account account(long balance) {
        return accounts.save(Account.builder().accountNumber(UUID.randomUUID().toString())
                .ownerName("Integrity test").balance(balance).build());
    }

    private long balance(Account account) { return accounts.findById(account.getId()).orElseThrow().getBalance(); }

    private BigInteger total() {
        return accounts.findAll().stream().map(a -> BigInteger.valueOf(a.getBalance()))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private TransferRequest request(Account from, Account to, long amount, String key) {
        TransferRequest r = new TransferRequest();
        r.setFromAccountId(from.getId()); r.setToAccountId(to.getId());
        r.setAmount(amount); r.setIdempotencyKey(key);
        return r;
    }
}
