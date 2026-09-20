package com.example.authservice.event;

/** Qeydiyyat tranzaksiyası uğurla bağlandıqdan sonra yayımlanır. */
public record UserRegisteredEvent(Long userId) {
}
