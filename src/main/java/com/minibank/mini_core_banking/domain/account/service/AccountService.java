package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.Account;
import com.minibank.mini_core_banking.domain.account.dto.CreateAccountRequest;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import com.minibank.mini_core_banking.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(CreateAccountRequest request) {

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 존재하는 계좌번호입니다.");
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
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
