package com.example.bookingservice.client;

import com.example.bookingservice.client.config.AuthServiceFeignConfig;
import com.example.bookingservice.client.dto.InternalUserSummaryDto;
import com.example.bookingservice.client.dto.UpdateArtistRatingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", url = "${services.auth-service.url}", configuration = AuthServiceFeignConfig.class)
public interface AuthServiceClient {

    @PatchMapping("/api/v1/artists/internal/{artistId}/rating")
    void updateArtistRating(@PathVariable("artistId") Long artistId, @RequestBody UpdateArtistRatingRequest request);

    @GetMapping("/api/v1/users/internal/{id}")
    InternalUserSummaryDto getUserSummary(@PathVariable("id") Long id);
}
