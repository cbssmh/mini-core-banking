package com.minibank.mini_core_banking.global;

import java.util.UUID;

public final class RequestIdHolder {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestIdHolder() {
    }

    public static void set(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getOrCreate() {
        String requestId = REQUEST_ID.get();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            REQUEST_ID.set(requestId);
        }
        return requestId;
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
