package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.CreateAccountRequest;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import com.minibank.mini_core_banking.domain.account.history.repository.TransferHistoryRepository;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransferHistoryRepository transferHistoryRepository;

    @Transactional
    public Account createAccount(CreateAccountRequest request) {

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new CustomException("이미 존재하는 계좌번호입니다.");
        }

        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .balance(request.getBalance())
                .ownerName(request.getOwnerName())
                .build();

        return accountRepository.save(account);
    }

    public List<Account> getAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new CustomException("계좌를 찾을 수 없습니다."));
    }

    public void transfer(Long fromId, Long toId, Long amount) {
        TransferHistory history = createPendingTransfer(fromId, toId, amount);

        try {
            processTransfer(history.getId(), fromId, toId, amount);
        } catch (Exception e) {
            markTransferFailed(history.getId());
            throw e;
        }
    }

    @Transactional
    public TransferHistory createPendingTransfer(Long fromId, Long toId, Long amount) {
        TransferHistory history = TransferHistory.builder()
                .fromAccountId(fromId)
                .toAccountId(toId)
                .amount(amount)
                .transferredAt(LocalDateTime.now())
                .status(TransferStatus.PENDING)
                .build();

        return transferHistoryRepository.save(history);
    }

    @Transactional
    public void processTransfer(Long historyId, Long fromId, Long toId, Long amount) {

        if (fromId.equals(toId)) {
            throw new CustomException("자기 자신에게 이체할 수 없습니다.");
        }

        if (amount == null || amount <= 0) {
            throw new CustomException("이체 금액은 0보다 커야 합니다.");
        }

        Account from = accountRepository.findByIdForUpdate(fromId)
                .orElseThrow(() -> new CustomException("출금 계좌 없음"));

        Account to = accountRepository.findByIdForUpdate(toId)
                .orElseThrow(() -> new CustomException("입금 계좌 없음"));

        if (from.getBalance() < amount) {
            throw new CustomException("잔액 부족");
        }

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        TransferHistory history = transferHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException("이체 기록 없음"));

        history.setStatus(TransferStatus.SUCCESS);
    }

    @Transactional
    public void markTransferFailed(Long historyId) {
        TransferHistory history = transferHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException("이체 기록 없음"));

        history.setStatus(TransferStatus.FAILED);
    }
}