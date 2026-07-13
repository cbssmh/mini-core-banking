package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class TransferApplicationServiceIntegrationTest {

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
    private TransferApplicationService transferApplicationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

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
    void transfersMoneyAndPreservesTotalAmount() {
        Account from = saveAccount("200-000-001", "Alice", 10_000L);
        Account to = saveAccount("200-000-002", "Bob", 5_000L);

        TransferResponse response = transferApplicationService.transfer(
                request(from.getId(), to.getId(), 3_000L, "transfer-key-001")
        );

        Account reloadedFrom = accountRepository.findById(from.getId()).orElseThrow();
        Account reloadedTo = accountRepository.findById(to.getId()).orElseThrow();

        assertThat(response.getTransferId()).isNotNull();
        assertThat(response.getStatus().name()).isEqualTo("SUCCESS");
        assertThat(reloadedFrom.getBalance()).isEqualTo(7_000L);
        assertThat(reloadedTo.getBalance()).isEqualTo(8_000L);
        assertThat(reloadedFrom.getBalance() + reloadedTo.getBalance()).isEqualTo(15_000L);
    }

    @Test
    void rollsBackWhenBalanceIsInsufficient() {
        Account from = saveAccount("200-000-003", "Carol", 1_000L);
        Account to = saveAccount("200-000-004", "Dave", 5_000L);

        assertThatThrownBy(() -> transferApplicationService.transfer(
                request(from.getId(), to.getId(), 2_000L, "transfer-key-002")
        )).isInstanceOf(CustomException.class);

        assertThat(accountRepository.findById(from.getId()).orElseThrow().getBalance()).isEqualTo(1_000L);
        assertThat(accountRepository.findById(to.getId()).orElseThrow().getBalance()).isEqualTo(5_000L);
        assertThat(transferHistoryRepository.findAll()).isEmpty();
    }

    @Test
    void rejectsSelfTransfer() {
        Account account = saveAccount("200-000-005", "Erin", 1_000L);

        assertThatThrownBy(() -> transferApplicationService.transfer(
                request(account.getId(), account.getId(), 100L, "transfer-key-003")
        )).isInstanceOf(CustomException.class);

        assertThat(accountRepository.findById(account.getId()).orElseThrow().getBalance()).isEqualTo(1_000L);
        assertThat(transferHistoryRepository.findAll()).isEmpty();
    }

    @Test
    void rollsBackWhenAccountDoesNotExist() {
        Account from = saveAccount("200-000-006", "Frank", 1_000L);

        assertThatThrownBy(() -> transferApplicationService.transfer(
                request(from.getId(), 999_999L, 100L, "transfer-key-004")
        )).isInstanceOf(RuntimeException.class);

        assertThat(accountRepository.findById(from.getId()).orElseThrow().getBalance()).isEqualTo(1_000L);
        assertThat(transferHistoryRepository.findAll()).isEmpty();
    }

    @Test
    void returnsExistingResultWithoutMovingMoneyAgainForSameIdempotencyKeyAndSameRequest() {
        Account from = saveAccount("200-000-007", "Grace", 10_000L);
        Account to = saveAccount("200-000-008", "Henry", 5_000L);
        TransferRequest request = request(from.getId(), to.getId(), 1_000L, "transfer-key-005");

        TransferResponse first = transferApplicationService.transfer(request);
        TransferResponse second = transferApplicationService.transfer(request);

        Account reloadedFrom = accountRepository.findById(from.getId()).orElseThrow();
        Account reloadedTo = accountRepository.findById(to.getId()).orElseThrow();

        assertThat(second.getTransferId()).isEqualTo(first.getTransferId());
        assertThat(reloadedFrom.getBalance()).isEqualTo(9_000L);
        assertThat(reloadedTo.getBalance()).isEqualTo(6_000L);
        assertThat(transferHistoryRepository.findAll()).hasSize(1);
    }

    @Test
    void rejectsSameIdempotencyKeyWithDifferentRequest() {
        Account from = saveAccount("200-000-009", "Ivy", 10_000L);
        Account to = saveAccount("200-000-010", "Jack", 5_000L);

        transferApplicationService.transfer(request(from.getId(), to.getId(), 1_000L, "transfer-key-006"));

        assertThatThrownBy(() -> transferApplicationService.transfer(
                request(from.getId(), to.getId(), 2_000L, "transfer-key-006")
        )).isInstanceOf(CustomException.class);

        assertThat(accountRepository.findById(from.getId()).orElseThrow().getBalance()).isEqualTo(9_000L);
        assertThat(accountRepository.findById(to.getId()).orElseThrow().getBalance()).isEqualTo(6_000L);
        assertThat(transferHistoryRepository.findAll()).hasSize(1);
    }

    @Test
    void ordersAccountIdsBeforeLocking() {
        assertThat(transferApplicationService.orderedAccountIds(20L, 10L))
                .containsExactly(10L, 20L);
    }

    @Test
    void handlesConcurrentSameIdempotencyRequestWithoutDoubleTransfer() throws Exception {
        Account from = saveAccount("200-000-011", "Kate", 10_000L);
        Account to = saveAccount("200-000-012", "Leo", 5_000L);
        TransferRequest request = request(from.getId(), to.getId(), 1_000L, "transfer-key-007");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<TransferResponse> task = () -> {
            start.await(5, TimeUnit.SECONDS);
            return transferApplicationService.transfer(request);
        };

        try {
            Future<TransferResponse> first = executor.submit(task);
            Future<TransferResponse> second = executor.submit(task);

            start.countDown();

            TransferResponse firstResponse = first.get(10, TimeUnit.SECONDS);
            TransferResponse secondResponse = second.get(10, TimeUnit.SECONDS);

            assertThat(secondResponse.getTransferId()).isEqualTo(firstResponse.getTransferId());
            assertThat(accountRepository.findById(from.getId()).orElseThrow().getBalance()).isEqualTo(9_000L);
            assertThat(accountRepository.findById(to.getId()).orElseThrow().getBalance()).isEqualTo(6_000L);
            assertThat(transferHistoryRepository.findAll()).hasSize(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Account saveAccount(String accountNumber, String ownerName, Long balance) {
        return accountRepository.save(Account.builder()
                .accountNumber(accountNumber)
                .ownerName(ownerName)
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
}
