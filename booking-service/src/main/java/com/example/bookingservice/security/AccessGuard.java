package com.example.bookingservice.security;

import com.example.bookingservice.exception.NotOwnerException;

/**
 * Sahiblik yoxlamalarının ortaq yeri.
 *
 * callerId həmişə gateway-in X-User-Id başlığından gəlir (bax GatewayHeaders).
 * null olması "kimlik naməlumdur" deməkdir və hər zaman rədd edilir - başlıq
 * gəlməyibsə (məsələn kimsə gateway-i keçib servisə birbaşa vurubsa) açıq
 * qalmamalıyıq.
 */
public final class AccessGuard {

    private AccessGuard() {
    }

    /** Yoldakı/parametrdəki id çağıranın öz id-si olmalıdır. */
    public static void requireSelf(Long resourceOwnerId, Long callerId) {
        if (callerId == null || !callerId.equals(resourceOwnerId)) {
            throw new NotOwnerException("Yalnız öz məlumatlarına baxa bilərsən.");
        }
    }

    /** Bronun iki tərəfindən biri olmalıdır - müştəri və ya usta. */
    public static void requireOneOf(Long callerId, Long first, Long second, String message) {
        if (callerId == null || (!callerId.equals(first) && !callerId.equals(second))) {
            throw new NotOwnerException(message);
        }
    }
}
