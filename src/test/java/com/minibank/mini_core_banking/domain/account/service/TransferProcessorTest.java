package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import com.minibank.mini_core_banking.global.RequestIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Executes real processor arithmetic; deliberately makes no database/locking claim. */
@ExtendWith(MockitoExtension.class)
class TransferProcessorTest {
    @Mock AccountRepository accounts;
    @Mock TransferHistoryRepository histories;
    TransferProcessor processor;
    Account from;
    Account to;

    @BeforeEach
    void setUp() {
        processor = new TransferProcessor(accounts, histories);
        from = Account.builder().id(1L).balance(1L).build();
        to = Account.builder().id(2L).balance(Long.MAX_VALUE).build();
        when(histories.findByIdempotencyKey("boundary")).thenReturn(Optional.empty());
        when(histories.save(any())).thenAnswer(call -> {
            TransferHistory history = call.getArgument(0);
            history.setId(1L);
            return history;
        });
        when(accounts.findAllByIdInForUpdate(List.of(1L, 2L))).thenReturn(List.of(from, to));
    }

    @AfterEach
    void clearRequestId() { RequestIdHolder.clear(); }

    @Test
    void rejectsDestinationOverflowBeforeMutatingEitherBalance() {
        TransferProcessingResult result = processor.process(request(1L));
        org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.outcome()).isEqualTo(TransferOutcome.FAILED);
            softly.assertThat(result.errorCode()).isEqualTo(ErrorCode.TRANSFER_FAILED);
            softly.assertThat(result.response().getStatus()).isEqualTo(TransferStatus.FAILED);
            softly.assertThat(from.getBalance()).isEqualTo(1L);
            softly.assertThat(to.getBalance()).isEqualTo(Long.MAX_VALUE);
        });
    }

    @Test
    void permitsExactMaximumAndConservesMathematicalTotal() {
        to.setBalance(Long.MAX_VALUE - 1);
        BigInteger before = total();
        assertThat(processor.process(request(1L)).response().getStatus()).isEqualTo(TransferStatus.SUCCESS);
        assertThat(from.getBalance()).isZero();
        assertThat(to.getBalance()).isEqualTo(Long.MAX_VALUE);
        assertThat(total()).isEqualTo(before);
    }

    @Test
    void permitsMaximumAmountIntoEmptyAccount() {
        from.setBalance(Long.MAX_VALUE);
        to.setBalance(0L);
        assertThat(processor.process(request(Long.MAX_VALUE)).response().getStatus()).isEqualTo(TransferStatus.SUCCESS);
        assertThat(from.getBalance()).isZero();
        assertThat(to.getBalance()).isEqualTo(Long.MAX_VALUE);
    }

    private BigInteger total() {
        return BigInteger.valueOf(from.getBalance()).add(BigInteger.valueOf(to.getBalance()));
    }

    private TransferRequest request(long amount) {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(amount);
        request.setIdempotencyKey("boundary");
        return request;
    }
}
