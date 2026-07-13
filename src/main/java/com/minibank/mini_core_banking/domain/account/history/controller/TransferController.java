package com.minibank.mini_core_banking.domain.account.history.controller;

import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.service.TransferHistoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferHistoryQueryService transferHistoryQueryService;

    @GetMapping("/transfers")
    public List<TransferHistory> getTransfers() {
        return transferHistoryQueryService.getTransfers();
    }

    @GetMapping("/transfers/account/{accountId}")
    public List<TransferHistory> getTransfersByAccount(@PathVariable Long accountId) {
        return transferHistoryQueryService.getTransfersByAccount(accountId);
    }
}
