package com.minibank.mini_core_banking.domain.account.history.controller;

import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferHistoryRepository transferHistoryRepository;

    @GetMapping("/transfers")
    public List<TransferHistory> getTransfers() {
        return transferHistoryRepository.findAllByOrderByIdDesc();
    }

    @GetMapping("/transfers/account/{accountId}")
    public List<TransferHistory> getTransfersByAccount(@PathVariable Long accountId) {
        return transferHistoryRepository
                .findByFromAccountIdOrToAccountIdOrderByIdDesc(accountId, accountId);
    }
}