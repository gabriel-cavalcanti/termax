package com.termax.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReadingRequest(
        @JsonAlias({"device_id", "device"}) String deviceId,
        Double temperature,
        Double humidity
) {
}
