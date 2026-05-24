package com.minibank.mini_core_banking.domain.account.history;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromAccountId;

    @Column(nullable = false)
    private Long toAccountId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private LocalDateTime transferredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;
}