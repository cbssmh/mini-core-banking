package com.minibank.mini_core_banking.global;

import java.time.LocalDateTime;

public record ErrorResponse(
        String requestId,
        String code,
        String message,
        LocalDateTime timestamp
) {
}
