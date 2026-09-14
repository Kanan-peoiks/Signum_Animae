package com.example.bookingservice.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Klientdən gələn ?page=&size= dəyərlərinin təhlükəsiz hala salınması.
 *  Məhdudiyyət olmasa "size=1000000" ilə bütün cədvəli bir sorğuda çəkmək olardı -
 *  səhifələmənin əsas məqsədi elə bunun qarşısını almaqdır. */
public final class PageParams {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PageParams() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort == null ? Sort.unsorted() : sort);
    }
}
