package com.minibank.mini_core_banking.domain.account.history;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transfer_history",
        indexes = {
                @Index(name = "idx_transfer_history_from_account_id", columnList = "from_account_id"),
                @Index(name = "idx_transfer_history_to_account_id", columnList = "to_account_id"),
                @Index(name = "idx_transfer_history_transferred_at", columnList = "transferred_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferStatus status;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;
}
