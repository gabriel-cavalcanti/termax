package com.termax.backend.dto;

public record ReadingAckResponse(
        String status,
        String timestamp,
        ReadingRequest received
) {
}
