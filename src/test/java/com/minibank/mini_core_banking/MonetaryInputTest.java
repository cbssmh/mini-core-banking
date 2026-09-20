package com.minibank.mini_core_banking;

import com.minibank.mini_core_banking.domain.account.controller.AccountController;
import com.minibank.mini_core_banking.domain.account.service.AccountService;
import com.minibank.mini_core_banking.domain.account.service.TransferApplicationService;
import com.minibank.mini_core_banking.global.GlobalExceptionHandler;
import com.minibank.mini_core_banking.global.RequestIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MVC binding, validation and advice; services are mocked to assert rejection before writes. */
class MonetaryInputTest {
    AccountService accounts;
    TransferApplicationService transfers;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountService.class);
        transfers = mock(TransferApplicationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AccountController(accounts, transfers))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void clearRequestId() { RequestIdHolder.clear(); }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null", "9223372036854775808", "\"not-money\""})
    void rejectsInvalidTransferAmountsBeforeCallingService(String amount) throws Exception {
        mvc.perform(post("/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":" + amount
                                + ",\"idempotencyKey\":\"input\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(transfers, accounts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "-1", "9223372036854775808", "\"not-money\""})
    void rejectsInvalidOpeningBalancesBeforeCallingService(String balance) throws Exception {
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"new\",\"ownerName\":\"test\",\"balance\":" + balance + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(transfers, accounts);
    }
}
