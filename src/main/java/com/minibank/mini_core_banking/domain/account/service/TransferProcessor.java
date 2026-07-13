package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import com.minibank.mini_core_banking.global.RequestIdHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferProcessor {

    private final AccountRepository accountRepository;
    private final TransferHistoryRepository transferHistoryRepository;

    @Transactional
    public TransferProcessingResult process(TransferRequest request) {
        transferHistoryRepository.acquireIdempotencyLock(request.getIdempotencyKey());

        return transferHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> handleDuplicate(existing, request))
                .orElseGet(() -> executeNewTransfer(request));
    }

    private TransferProcessingResult handleDuplicate(TransferHistory existing, TransferRequest request) {
        if (!isSameRequest(existing, request)) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }

        return new TransferProcessingResult(TransferResponse.from(existing), TransferOutcome.REPLAY);
    }

    private TransferProcessingResult executeNewTransfer(TransferRequest request) {
        LocalDateTime now = LocalDateTime.now();
        TransferHistory history = transferHistoryRepository.save(TransferHistory.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .idempotencyKey(request.getIdempotencyKey())
                .requestId(RequestIdHolder.getOrCreate())
                .status(TransferStatus.PENDING)
                .transferredAt(now)
                .build());

        List<Account> lockedAccounts = lockAccountsInDeterministicOrder(
                request.getFromAccountId(),
                request.getToAccountId()
        );

        Account from = findLockedAccount(lockedAccounts, request.getFromAccountId());
        Account to = findLockedAccount(lockedAccounts, request.getToAccountId());

        if (from.getBalance() < request.getAmount()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        from.setBalance(from.getBalance() - request.getAmount());
        to.setBalance(to.getBalance() + request.getAmount());

        history.setStatus(TransferStatus.SUCCESS);
        history.setCompletedAt(LocalDateTime.now());

        return new TransferProcessingResult(TransferResponse.from(history), TransferOutcome.SUCCESS);
    }

    List<Account> lockAccountsInDeterministicOrder(Long firstAccountId, Long secondAccountId) {
        return accountRepository.findAllByIdInForUpdate(orderedAccountIds(firstAccountId, secondAccountId));
    }

    private Account findLockedAccount(List<Account> accounts, Long accountId) {
        return accounts.stream()
                .filter(account -> account.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private boolean isSameRequest(TransferHistory existing, TransferRequest request) {
        return existing.getFromAccountId().equals(request.getFromAccountId())
                && existing.getToAccountId().equals(request.getToAccountId())
                && existing.getAmount().equals(request.getAmount());
    }

    List<Long> orderedAccountIds(Long firstAccountId, Long secondAccountId) {
        return List.of(firstAccountId, secondAccountId).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
