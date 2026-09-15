package com.example.notificationservice.security;

import com.example.notificationservice.exception.NotOwnerException;

public final class AccessGuard {

    private AccessGuard() {
    }

    public static void requireSelf(Long resourceOwnerId, Long callerId) {
        if (callerId == null || !callerId.equals(resourceOwnerId)) {
            throw new NotOwnerException("Yalnız öz məlumatlarına baxa bilərsən.");
        }
    }

    public static void requireOneOf(Long callerId, Long first, Long second, String message) {
        if (callerId == null || (!callerId.equals(first) && !callerId.equals(second))) {
            throw new NotOwnerException(message);
        }
    }
}
