package com.example.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/** Səhifələnmiş cavabın sabit forması.
 *
 *  Spring-in öz {@code Page} obyektini birbaşa JSON-a çevirmirik: onun sahə quruluşu
 *  Spring versiyasından/serializasiya rejimindən asılı olaraq dəyişir. Frontend-in
 *  gözlədiyi sahələri (content/totalElements/totalPages/number) burada özümüz təsbit
 *  edirik ki, bütün servislərdə eyni olsun. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    /** Cari səhifənin nömrəsi - 0-dan başlayır. */
    private int number;
    private int size;
    private boolean last;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .number(page.getNumber())
                .size(page.getSize())
                .last(page.isLast())
                .build();
    }
}
