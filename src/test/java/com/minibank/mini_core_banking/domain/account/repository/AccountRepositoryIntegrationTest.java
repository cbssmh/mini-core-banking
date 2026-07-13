package com.minibank.mini_core_banking.domain.account.repository;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryIntegrationTest {

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
    private AccountRepository accountRepository;

    @Autowired
    private TransferHistoryRepository transferHistoryRepository;

    @Test
    void savesAccount() {
        Account saved = accountRepository.save(Account.builder()
                .accountNumber("100-000-001")
                .ownerName("Alice")
                .balance(10_000L)
                .build());

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findsAccountByAccountNumber() {
        accountRepository.save(Account.builder()
                .accountNumber("100-000-002")
                .ownerName("Bob")
                .balance(20_000L)
                .build());

        assertThat(accountRepository.findByAccountNumber("100-000-002"))
                .isPresent()
                .get()
                .extracting(Account::getOwnerName)
                .isEqualTo("Bob");
    }

    @Test
    void rejectsDuplicateAccountNumber() {
        accountRepository.saveAndFlush(Account.builder()
                .accountNumber("100-000-003")
                .ownerName("Carol")
                .balance(30_000L)
                .build());

        Account duplicate = Account.builder()
                .accountNumber("100-000-003")
                .ownerName("Dave")
                .balance(40_000L)
                .build();

        assertThatThrownBy(() -> accountRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savesTransferHistory() {
        Account from = saveAccount("100-000-004", "Erin", 50_000L);
        Account to = saveAccount("100-000-005", "Frank", 60_000L);

        TransferHistory saved = transferHistoryRepository.save(TransferHistory.builder()
                .fromAccountId(from.getId())
                .toAccountId(to.getId())
                .amount(5_000L)
                .status(TransferStatus.SUCCESS)
                .transferredAt(LocalDateTime.now())
                .build());

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void enforcesTransferHistoryForeignKeys() {
        TransferHistory invalid = TransferHistory.builder()
                .fromAccountId(999_001L)
                .toAccountId(999_002L)
                .amount(5_000L)
                .status(TransferStatus.SUCCESS)
                .transferredAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> transferHistoryRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Account saveAccount(String accountNumber, String ownerName, Long balance) {
        return accountRepository.save(Account.builder()
                .accountNumber(accountNumber)
                .ownerName(ownerName)
                .balance(balance)
                .build());
    }
}
