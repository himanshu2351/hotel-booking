package com.project.hotel_booking.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiIndexControllerTest {
    @Test
    void listsSwaggerAndApiDocumentationLinks() {
        var response = new ApiIndexController().index();

        assertThat(response)
                .containsEntry("name", "Hotel Booking API")
                .containsEntry("swaggerUi", "/swagger-ui.html")
                .containsEntry("apiDocumentation", "/v3/api-docs");
    }
}
