package com.minibank.mini_core_banking;

import com.minibank.mini_core_banking.domain.account.controller.AccountController;
import com.minibank.mini_core_banking.domain.account.service.AccountService;
import com.minibank.mini_core_banking.domain.account.service.TransferApplicationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses the application's Jackson configuration, unlike standalone MVC fixtures. */
@WebMvcTest(AccountController.class)
class MonetaryJsonBindingTest {
    @Autowired MockMvc mvc;
    @MockitoBean AccountService accounts;
    @MockitoBean TransferApplicationService transfers;

    @Test
    void retainsExactMaximumIntegerTransferAmount() throws Exception {
        mvc.perform(post("/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":9223372036854775807,"
                                + "\"idempotencyKey\":\"maximum\"}"))
                .andExpect(status().isOk());
        verify(transfers).transfer(argThat(r -> r.getAmount().equals(Long.MAX_VALUE)));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, Long.MAX_VALUE})
    void retainsValidIntegerOpeningBalances(long balance) throws Exception {
        when(accounts.createAccount(any())).thenAnswer(call -> {
            com.minibank.mini_core_banking.domain.account.dto.CreateAccountRequest r = call.getArgument(0);
            return com.minibank.mini_core_banking.domain.account.Account.builder()
                    .id(1L).balance(r.getBalance()).accountNumber(r.getAccountNumber())
                    .ownerName(r.getOwnerName()).build();
        });
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"valid\",\"ownerName\":\"test\",\"balance\":" + balance + "}"))
                .andExpect(status().isOk());
        verify(accounts).createAccount(argThat(r -> r.getBalance().equals(balance)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "-0.5", "1e3"})
    void rejectsNonIntegerTransferTokensWithoutTruncation(String amount) throws Exception {
        java.util.concurrent.atomic.AtomicReference<Long> received = new java.util.concurrent.atomic.AtomicReference<>();
        when(transfers.transfer(any())).thenAnswer(call -> {
            com.minibank.mini_core_banking.domain.account.dto.TransferRequest r = call.getArgument(0);
            received.set(r.getAmount());
            return null;
        });
        var result = mvc.perform(post("/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":" + amount
                                + ",\"idempotencyKey\":\"fraction\"}"))
                .andReturn();
        org.assertj.core.api.SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getResponse().getStatus()).isEqualTo(400);
            softly.assertThat(received.get()).as("amount passed to the service").isNull();
        });
        assertThat(result.getResponse().getContentAsString()).contains("INVALID_REQUEST");
        verifyNoInteractions(transfers, accounts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "-0.5"})
    void rejectsNonIntegerOpeningBalanceTokensWithoutTruncation(String amount) throws Exception {
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"new\",\"ownerName\":\"test\",\"balance\":" + amount + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(transfers, accounts);
    }
}
