package com.example.bookingservice.config;

/**
 * Gateway-in təyin etdiyi etibarlı başlıqlar.
 *
 * Bu başlığı klient QOYA BİLMƏZ: gateway-in JwtAuthenticationFilter-i klientdən gələn
 * eyni adlı başlığı həmişə əvvəlcə silir və yalnız JWT doğrulandıqdan sonra öz dəyərini
 * yazır. Ona görə çağıranın kimliyi üçün yeganə etibarlı mənbə budur - sorğunun
 * gövdəsindəki, yolundakı və ya parametrindəki id-lər deyil.
 */
public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";

    private GatewayHeaders() {
    }
}
