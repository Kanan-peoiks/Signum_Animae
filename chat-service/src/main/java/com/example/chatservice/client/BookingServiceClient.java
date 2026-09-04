package com.example.chatservice.client;

import com.example.chatservice.client.config.BookingServiceFeignConfig;
import com.example.chatservice.client.dto.BookingStatusDto;
import com.example.chatservice.client.dto.UpdateBookingPriceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "booking-service", url = "${services.booking-service.url}", configuration = BookingServiceFeignConfig.class)
public interface BookingServiceClient {

    @PatchMapping("/api/v1/bookings/internal/{bookingId}/price")
    void updatePrice(@PathVariable("bookingId") Long bookingId, @RequestBody UpdateBookingPriceRequest request);

    /** Used to block new price OFFERs (and accepting existing ones) once the underlying
     *  booking has been cancelled - see ChatMessageService. */
    @GetMapping("/api/v1/bookings/internal/{bookingId}")
    BookingStatusDto getBookingStatus(@PathVariable("bookingId") Long bookingId);
}
