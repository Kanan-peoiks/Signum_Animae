package com.example.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferStatsResponse {
    private long totalOffers;
    private long accepted;
    private long rejected;
    private long pending;
    private double acceptanceRate;
}
