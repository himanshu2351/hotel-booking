package com.project.hotel_booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiIndexController {
    @GetMapping
    public Map<String, String> index() {
        return Map.of(
                "name", "Hotel Booking API",
                "swaggerUi", "/swagger-ui.html",
                "apiDocumentation", "/v3/api-docs"
        );
    }
}
