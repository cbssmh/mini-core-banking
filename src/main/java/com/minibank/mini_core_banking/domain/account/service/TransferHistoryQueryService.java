package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferHistoryQueryService {

    private final TransferHistoryRepository transferHistoryRepository;

    public List<TransferHistory> getTransfers() {
        return transferHistoryRepository.findAllByOrderByIdDesc();
    }

    public List<TransferHistory> getTransfersByAccount(Long accountId) {
        return transferHistoryRepository.findByFromAccountIdOrToAccountIdOrderByIdDesc(accountId, accountId);
    }

    public List<TransferHistory> getFailedTransfersForReconciliation() {
        return transferHistoryRepository.findByStatusOrderByIdDesc(TransferStatus.FAILED);
    }
}
