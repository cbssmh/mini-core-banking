package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.TransferRequest;
import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferApplicationService {

    private final AccountRepository accountRepository;
    private final TransferHistoryRepository transferHistoryRepository;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        validateRequest(request);
        transferHistoryRepository.acquireIdempotencyLock(request.getIdempotencyKey());

        return transferHistoryRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> handleDuplicate(existing, request))
                .orElseGet(() -> executeNewTransfer(request));
    }

    private void validateRequest(TransferRequest request) {
        if (request.getFromAccountId() == null || request.getToAccountId() == null) {
            throw new CustomException("계좌 ID는 필수입니다.");
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new CustomException("이체 금액은 0보다 커야 합니다.");
        }

        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().isBlank()) {
            throw new CustomException("Idempotency key is required");
        }

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new CustomException("자기 자신에게 이체할 수 없습니다.");
        }
    }

    private TransferResponse handleDuplicate(TransferHistory existing, TransferRequest request) {
        if (!isSameRequest(existing, request)) {
            throw new CustomException("Idempotency key conflict");
        }

        return TransferResponse.from(existing);
    }

    private TransferResponse executeNewTransfer(TransferRequest request) {
        TransferHistory history = transferHistoryRepository.save(TransferHistory.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .idempotencyKey(request.getIdempotencyKey())
                .status(TransferStatus.PENDING)
                .transferredAt(LocalDateTime.now())
                .build());

        List<Account> lockedAccounts = lockAccountsInDeterministicOrder(
                request.getFromAccountId(),
                request.getToAccountId()
        );

        Account from = findLockedAccount(lockedAccounts, request.getFromAccountId(), "출금 계좌 없음");
        Account to = findLockedAccount(lockedAccounts, request.getToAccountId(), "입금 계좌 없음");

        if (from.getBalance() < request.getAmount()) {
            throw new CustomException("잔액 부족");
        }

        from.setBalance(from.getBalance() - request.getAmount());
        to.setBalance(to.getBalance() + request.getAmount());

        history.setStatus(TransferStatus.SUCCESS);

        return TransferResponse.from(history);
    }

    List<Account> lockAccountsInDeterministicOrder(Long firstAccountId, Long secondAccountId) {
        List<Long> orderedIds = List.of(firstAccountId, secondAccountId).stream()
                .sorted()
                .toList();

        return accountRepository.findAllByIdInForUpdate(orderedIds);
    }

    private Account findLockedAccount(List<Account> accounts, Long accountId, String message) {
        return accounts.stream()
                .filter(account -> account.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new CustomException(message));
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
