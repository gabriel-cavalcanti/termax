package com.termax.backend.controller;

import com.termax.backend.dto.DeviceTelemetry;
import com.termax.backend.dto.ReadingAckResponse;
import com.termax.backend.dto.ReadingRequest;
import com.termax.backend.dto.TelemetryResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class TesteController {

    private static final Logger log = LoggerFactory.getLogger(TesteController.class);
    private static final int MAX_READINGS = 15;
    private static final double MIN_TEMP = -50.0;
    private static final double MAX_TEMP = 100.0;
    private static final double MIN_HUM = 0.0;
    private static final double MAX_HUM = 100.0;
    private static final String DEFAULT_DEVICE_ID = "esp32-dht22";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private volatile String lastDevice = DEFAULT_DEVICE_ID;
    private volatile String lastUpdate = "10:05:00";
    private volatile boolean usingInitialMock = true;

    private final List<String> labels = new CopyOnWriteArrayList<>();
    private final List<Double> temperature = new CopyOnWriteArrayList<>();
    private final List<Double> humidity = new CopyOnWriteArrayList<>();

    private final Map<String, DeviceBuffer> deviceBuffers = new ConcurrentHashMap<>();

    public TesteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        carregarMockInicial();
    }

    private synchronized void carregarMockInicial() {
        List<String> mockLabels = List.of("10:00", "10:01", "10:02", "10:03", "10:04", "10:05");
        List<Double> mockTemp = List.of(24.5, 24.8, 25.1, 25.4, 25.2, 25.6);
        List<Double> mockHum = List.of(58.0, 57.5, 57.0, 56.5, 56.0, 55.5);

        labels.clear();
        labels.addAll(mockLabels);
        temperature.clear();
        temperature.addAll(mockTemp);
        humidity.clear();
        humidity.addAll(mockHum);

        deviceBuffers.clear();
        DeviceBuffer esp32Mock = new DeviceBuffer(DEFAULT_DEVICE_ID);
        for (int i = 0; i < mockLabels.size(); i++) {
            esp32Mock.addReading(mockLabels.get(i), mockTemp.get(i), mockHum.get(i));
        }
        deviceBuffers.put(DEFAULT_DEVICE_ID, esp32Mock);

        this.lastDevice = DEFAULT_DEVICE_ID;
        this.lastUpdate = "10:05:00";
        this.usingInitialMock = true;
    }

    @GetMapping({"/teste", "/readings"})
    public ResponseEntity<TelemetryResponse> testarConexao() {
        Map<String, DeviceTelemetry> devicesSnapshot = construirSnapshotDispositivos();

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            TelemetryResponse response = new TelemetryResponse(
                    "UP",
                    "PostgreSQL connected",
                    lastDevice,
                    lastUpdate,
                    labels.size(),
                    List.copyOf(labels),
                    List.copyOf(temperature),
                    List.copyOf(humidity),
                    devicesSnapshot
            );
            return ResponseEntity.ok(response);
        } catch (DataAccessException ex) {
            log.warn("Falha na verificacao de saude do PostgreSQL: {}", ex.getMessage());
            TelemetryResponse degradedResponse = new TelemetryResponse(
                    "DEGRADED",
                    "PostgreSQL disconnected",
                    lastDevice,
                    lastUpdate,
                    labels.size(),
                    List.copyOf(labels),
                    List.copyOf(temperature),
                    List.copyOf(humidity),
                    devicesSnapshot
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(degradedResponse);
        }
    }

    @PostMapping({"/teste", "/readings"})
    public ResponseEntity<?> receberLeitura(@RequestBody(required = false) ReadingRequest payload) {
        if (payload == null || payload.temperature() == null || payload.humidity() == null) {
            log.warn("Payload invalido recebido: campos temperature e humidity sao obrigatorios. Payload={}", payload);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "BAD_REQUEST",
                    "message", "Os campos 'temperature' e 'humidity' sao obrigatorios."
            ));
        }

        double tempVal = payload.temperature();
        double humVal = payload.humidity();

        if (Double.isNaN(tempVal) || Double.isInfinite(tempVal) || tempVal < MIN_TEMP || tempVal > MAX_TEMP) {
            log.warn("Temperatura fora dos limites fisicos plausiveis ({} a {} C): {}", MIN_TEMP, MAX_TEMP, tempVal);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "BAD_REQUEST",
                    "message", "Temperatura fora dos limites fisicos plausiveis (-50.0 a 100.0 C)."
            ));
        }

        if (Double.isNaN(humVal) || Double.isInfinite(humVal) || humVal < MIN_HUM || humVal > MAX_HUM) {
            log.warn("Umidade fora dos limites fisicos plausiveis ({} a {} %): {}", MIN_HUM, MAX_HUM, humVal);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "BAD_REQUEST",
                    "message", "Umidade fora dos limites fisicos plausiveis (0.0 a 100.0 %)."
            ));
        }

        String resolvedDevice = (payload.deviceId() == null || payload.deviceId().isBlank())
                ? DEFAULT_DEVICE_ID
                : payload.deviceId().trim();
        String timestamp = LocalTime.now().format(TIME_FORMATTER);

        synchronized (this) {
            if (usingInitialMock) {
                labels.clear();
                temperature.clear();
                humidity.clear();
                deviceBuffers.clear();
                usingInitialMock = false;
                log.info("Primeira leitura real recebida: dados mockados iniciais substituidos por telemetria real.");
            }

            this.lastDevice = resolvedDevice;
            this.lastUpdate = timestamp;

            labels.add(timestamp);
            temperature.add(tempVal);
            humidity.add(humVal);

            while (labels.size() > MAX_READINGS) {
                labels.remove(0);
            }
            while (temperature.size() > MAX_READINGS) {
                temperature.remove(0);
            }
            while (humidity.size() > MAX_READINGS) {
                humidity.remove(0);
            }

            DeviceBuffer buffer = deviceBuffers.computeIfAbsent(resolvedDevice, DeviceBuffer::new);
            buffer.addReading(timestamp, tempVal, humVal);
        }

        log.info(">>> [ESP POST RECEBIDO] device={} temp={}C hum={}%", resolvedDevice, tempVal, humVal);

        ReadingRequest normalizedRequest = new ReadingRequest(resolvedDevice, tempVal, humVal);
        ReadingAckResponse ack = new ReadingAckResponse("CREATED", timestamp, normalizedRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(ack);
    }

    @DeleteMapping({"/readings", "/teste"})
    public synchronized ResponseEntity<Map<String, Object>> limparHistorico() {
        labels.clear();
        temperature.clear();
        humidity.clear();
        deviceBuffers.clear();
        usingInitialMock = false;
        lastUpdate = LocalTime.now().format(TIME_FORMATTER);

        log.info(">>> [HISTORICO LIMPO] Buffers de telemetria em memoria zerados pelo usuario.");

        return ResponseEntity.ok(Map.of(
                "status", "CLEARED",
                "timestamp", lastUpdate,
                "message", "Historico de leituras e dados mockados removidos com sucesso."
        ));
    }

    private synchronized Map<String, DeviceTelemetry> construirSnapshotDispositivos() {
        Map<String, DeviceTelemetry> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, DeviceBuffer> entry : deviceBuffers.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().toDto());
        }
        return snapshot;
    }

    private static final class DeviceBuffer {
        private final String deviceId;
        private final List<String> labels = new ArrayList<>();
        private final List<Double> temperature = new ArrayList<>();
        private final List<Double> humidity = new ArrayList<>();
        private Double lastTemperature;
        private Double lastHumidity;
        private String lastUpdate = "--:--:--";

        private DeviceBuffer(String deviceId) {
            this.deviceId = deviceId;
        }

        private void addReading(String timestamp, double temp, double hum) {
            labels.add(timestamp);
            temperature.add(temp);
            humidity.add(hum);
            lastTemperature = temp;
            lastHumidity = hum;
            lastUpdate = timestamp;

            while (labels.size() > MAX_READINGS) {
                labels.remove(0);
            }
            while (temperature.size() > MAX_READINGS) {
                temperature.remove(0);
            }
            while (humidity.size() > MAX_READINGS) {
                humidity.remove(0);
            }
        }

        private DeviceTelemetry toDto() {
            return new DeviceTelemetry(
                    deviceId,
                    lastTemperature,
                    lastHumidity,
                    lastUpdate,
                    labels.size(),
                    List.copyOf(labels),
                    List.copyOf(temperature),
                    List.copyOf(humidity)
            );
        }
    }
}
