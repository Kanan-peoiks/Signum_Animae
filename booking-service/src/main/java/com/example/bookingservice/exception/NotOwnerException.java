package com.example.bookingservice.exception;

/**
 * Çağıran tanınır, amma istədiyi resurs ona aid deyil (403).
 *
 * Domenə xas ReviewOwnershipException/AvailabilityOwnershipException-dan fərqi:
 * bu, yoldakı və ya parametrdəki id-nin doğrulanmış çağıranla üst-üstə düşmədiyi
 * ümumi haldır - məsələn başqasının sifariş siyahısını istəmək.
 */
public class NotOwnerException extends RuntimeException {
    public NotOwnerException(String message) {
        super(message);
    }
}
