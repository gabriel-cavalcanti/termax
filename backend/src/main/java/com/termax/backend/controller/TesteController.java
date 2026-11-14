package com.termax.backend.controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class TesteController {

    private static final int MAX_READINGS = 15;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    private final List<String> labels = new CopyOnWriteArrayList<>(
            List.of("10:00", "10:01", "10:02", "10:03", "10:04", "10:05")
    );
    private final List<Double> temperature = new CopyOnWriteArrayList<>(
            List.of(24.5, 24.8, 25.1, 25.4, 25.2, 25.6)
    );
    private final List<Double> humidity = new CopyOnWriteArrayList<>(
            List.of(58.0, 57.5, 57.0, 56.5, 56.0, 55.5)
    );

    public TesteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/teste")
    public ResponseEntity<Map<String, Object>> testarConexao() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("database", "PostgreSQL connected");
        response.put("labels", labels);
        response.put("temperature", temperature);
        response.put("humidity", humidity);

        return ResponseEntity.ok(response);
    }

    @PostMapping({"/teste", "/readings"})
    public ResponseEntity<Map<String, Object>> receberLeitura(@RequestBody Map<String, Object> payload) {
        System.out.println(">>> [ESP POST RECEBIDO] " + payload);

        Double tempVal = extrairDouble(payload.get("temperature"));
        Double humVal = extrairDouble(payload.get("humidity"));

        if (tempVal != null && humVal != null) {
            synchronized (this) {
                labels.add(LocalTime.now().format(TIME_FORMATTER));
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
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "CREATED");
        response.put("received", payload);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Double extrairDouble(Object valor) {
        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }
        if (valor != null) {
            try {
                return Double.parseDouble(valor.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

