package com.minibank.mini_core_banking;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import com.minibank.mini_core_banking.domain.account.service.TransferApplicationService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ObservabilityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TransferApplicationService transferApplicationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeEach
    void setUp() {
        transferHistoryRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        transferHistoryRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @Test
    void meterRegistryExists() {
        assertThat(meterRegistry).isNotNull();
    }

    @Test
    void recordsAttemptSuccessAndDurationForNewTransfer() {
        Account from = saveAccount(10_000L);
        Account to = saveAccount(5_000L);
        double attempts = counter("bank.transfer.attempts");
        double success = counter("bank.transfer.success");
        long durationCount = timerCount();

        transferApplicationService.transfer(request(from.getId(), to.getId(), 1_000L, key()));

        assertThat(counter("bank.transfer.attempts")).isEqualTo(attempts + 1);
        assertThat(counter("bank.transfer.success")).isEqualTo(success + 1);
        assertThat(timerCount()).isGreaterThan(durationCount);
    }

    @Test
    void recordsAttemptFailedAndDurationForInsufficientBalance() {
        Account from = saveAccount(1_000L);
        Account to = saveAccount(5_000L);
        double attempts = counter("bank.transfer.attempts");
        double failed = counter("bank.transfer.failed");
        long durationCount = timerCount();

        assertThatThrownBy(() -> transferApplicationService.transfer(
                request(from.getId(), to.getId(), 2_000L, key())
        )).isInstanceOf(CustomException.class);

        assertThat(counter("bank.transfer.attempts")).isEqualTo(attempts + 1);
        assertThat(counter("bank.transfer.failed")).isEqualTo(failed + 1);
        assertThat(timerCount()).isGreaterThan(durationCount);
    }

    @Test
    void recordsIdempotencyReplayCounter() {
        Account from = saveAccount(10_000L);
        Account to = saveAccount(5_000L);
        TransferRequest request = request(from.getId(), to.getId(), 1_000L, key());
        transferApplicationService.transfer(request);
        double replay = counter("bank.transfer.idempotency.replay");

        transferApplicationService.transfer(request);

        assertThat(counter("bank.transfer.idempotency.replay")).isEqualTo(replay + 1);
    }

    @Test
    void recordsIdempotencyConflictCounter() {
        Account from = saveAccount(10_000L);
        Account to = saveAccount(5_000L);
        String key = key();
        transferApplicationService.transfer(request(from.getId(), to.getId(), 1_000L, key));
        double conflict = counter("bank.transfer.idempotency.conflict");

        assertThatThrownBy(() -> transferApplicationService.transfer(
                request(from.getId(), to.getId(), 2_000L, key)
        )).isInstanceOf(CustomException.class);

        assertThat(counter("bank.transfer.idempotency.conflict")).isEqualTo(conflict + 1);
    }

    @Test
    void validationFailureDoesNotIncreaseBusinessFailedCounter() {
        Account account = saveAccount(10_000L);
        double failed = counter("bank.transfer.failed");
        double attempts = counter("bank.transfer.attempts");

        assertThatThrownBy(() -> transferApplicationService.transfer(
                request(account.getId(), account.getId(), 100L, key())
        )).isInstanceOf(CustomException.class);

        assertThat(counter("bank.transfer.failed")).isEqualTo(failed);
        assertThat(counter("bank.transfer.attempts")).isEqualTo(attempts);
    }

    @Test
    void actuatorHealthEndpointResponds() throws Exception {
        HttpResponse<String> response = get("/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void prometheusEndpointExposesMetricText() throws Exception {
        Account from = saveAccount(10_000L);
        Account to = saveAccount(5_000L);
        transferApplicationService.transfer(request(from.getId(), to.getId(), 1_000L, key()));

        HttpResponse<String> response = get("/actuator/prometheus");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("bank_transfer_attempts_total");
        assertThat(response.body()).contains("bank_transfer_duration_seconds_count");
        assertThat(response.body()).contains("jvm_memory_used_bytes");
        assertThat(response.body()).contains("jdbc_connections_active");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private double counter(String name) {
        return meterRegistry.find(name).counter().count();
    }

    private long timerCount() {
        long count = 0L;
        for (Timer timer : meterRegistry.find("bank.transfer.duration").timers()) {
            count += timer.count();
        }
        return count;
    }

    private Account saveAccount(Long balance) {
        return accountRepository.save(Account.builder()
                .accountNumber("obs-" + UUID.randomUUID())
                .ownerName("Observability")
                .balance(balance)
                .build());
    }

    private TransferRequest request(Long fromAccountId, Long toAccountId, Long amount, String idempotencyKey) {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(amount);
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }

    private String key() {
        return "obs-key-" + UUID.randomUUID();
    }
}
