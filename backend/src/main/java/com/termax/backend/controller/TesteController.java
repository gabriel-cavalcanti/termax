package com.termax.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class TesteController {

    private final JdbcTemplate jdbcTemplate;

    public TesteController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/teste")
    public ResponseEntity<Map<String, Object>> testarConexao() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("database", "PostgreSQL connected");
        response.put("labels", List.of("10:00", "10:01", "10:02", "10:03", "10:04", "10:05"));
        response.put("temperature", List.of(24.5, 24.8, 25.1, 25.4, 25.2, 25.6));
        response.put("humidity", List.of(58.0, 57.5, 57.0, 56.5, 56.0, 55.5));

        return ResponseEntity.ok(response);
    }
}
