package com.termax.backend.dto;

import java.util.List;

public record DeviceTelemetry(
        String deviceId,
        Double lastTemperature,
        Double lastHumidity,
        String lastUpdate,
        int totalReadings,
        List<String> labels,
        List<Double> temperature,
        List<Double> humidity
) {
}
