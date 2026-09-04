package com.example.bookingservice.client;

import com.example.bookingservice.client.config.AuthServiceFeignConfig;
import com.example.bookingservice.client.dto.InternalUserSummaryDto;
import com.example.bookingservice.client.dto.UpdateArtistRatingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Direct service-to-service calls to auth-service, bypassing the gateway entirely
 * (server-to-server traffic, not a request carrying a user's JWT). Both endpoints are
 * under /internal/ on auth-service and require the shared X-Internal-Token, attached
 * automatically to every call by AuthServiceFeignConfig.
 */
@FeignClient(name = "auth-service", url = "${services.auth-service.url}", configuration = AuthServiceFeignConfig.class)
public interface AuthServiceClient {

    @PatchMapping("/api/v1/artists/internal/{artistId}/rating")
    void updateArtistRating(@PathVariable("artistId") Long artistId, @RequestBody UpdateArtistRatingRequest request);

    /** Used to decide, server-side, whether a viewer sees a past tattoo's artist by full
     *  name or only initials (premium gating) - see BookingService.getCompletedSummaryForCustomer. */
    @GetMapping("/api/v1/users/internal/{id}")
    InternalUserSummaryDto getUserSummary(@PathVariable("id") Long id);
}
