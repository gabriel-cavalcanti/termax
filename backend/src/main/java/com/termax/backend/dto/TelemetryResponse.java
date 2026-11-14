package com.termax.backend.dto;

import java.util.List;
import java.util.Map;

public record TelemetryResponse(
        String status,
        String database,
        String lastDevice,
        String lastUpdate,
        int totalReadings,
        List<String> labels,
        List<Double> temperature,
        List<Double> humidity,
        Map<String, DeviceTelemetry> devices
) {
}
