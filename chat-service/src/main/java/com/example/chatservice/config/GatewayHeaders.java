package com.example.chatservice.config;

/**
 * Gateway-in təyin etdiyi etibarlı başlıqlar.
 *
 * Bu başlığı klient QOYA BİLMƏZ: gateway-in JwtAuthenticationFilter-i klientdən gələn
 * eyni adlı başlığı həmişə əvvəlcə silir və yalnız JWT doğrulandıqdan sonra öz dəyərini
 * yazır. Ona görə chat-service-də çağıranın kimliyi üçün yeganə etibarlı mənbə budur -
 * sorğunun gövdəsindəki və ya yolundakı id-lər deyil.
 */
public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";

    private GatewayHeaders() {
    }
}
