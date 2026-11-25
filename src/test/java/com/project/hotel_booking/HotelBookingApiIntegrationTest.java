package com.project.hotel_booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HotelBookingApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void supportsPropertyDiscoveryBookingPaymentIdempotencyAndCancellation() throws Exception {
        mockMvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hotel Booking API"))
                .andExpect(jsonPath("$.swaggerUi").value("/swagger-ui.html"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("malformed.json")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request is invalid or incomplete"));
        mockMvc.perform(get("/api/bookings/not-a-uuid"))
                .andExpect(status().isBadRequest());

        MvcResult invalidOwner = mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("owner-invalid.json")))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(invalidOwner.getResponse().getContentAsString()).contains("Owner name is required");

        MvcResult ownerResult = mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("owner-valid.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties").isEmpty())
                .andReturn();
        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("owner-duplicate.json")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An owner with this email already exists"));
        String portfolioId = readId(ownerResult.getResponse().getContentAsString(), "id");

        mockMvc.perform(post("/api/portfolios/00000000-0000-0000-0000-000000000000/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("property-valid.json")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/portfolios/{portfolioId}/properties", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("property-invalid.json")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/portfolios/{portfolioId}/properties", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("property-null-room.json")))
                .andExpect(status().isBadRequest());

        MvcResult propertyResult = mockMvc.perform(post("/api/portfolios/{portfolioId}/properties", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("property-valid.json")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties.length()").value(1))
                .andReturn();
        String propertyJson = propertyResult.getResponse().getContentAsString();
        String propertyId = readNamedId(propertyJson, "Harbor View Hotel");
        String familyRoomTypeId = readNamedId(propertyJson, "Family");

        mockMvc.perform(get("/api/portfolios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerName").value("Asha Mehta"));
        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId));
        mockMvc.perform(get("/api/portfolios/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/properties")
                        .param("city", "mumb")
                        .param("locality", "COL")
                        .param("checkIn", "2030-06-10")
                        .param("checkOut", "2030-06-12")
                        .param("guests", "2")
                        .param("minPriceMinor", "11000")
                        .param("maxPriceMinor", "13000")
                        .param("minStarRating", "4")
                        .param("amenities", "WIFI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(propertyId));

        // The cheap Economy room cannot host two guests, and the Family room is outside this price range.
        mockMvc.perform(get("/api/properties")
                        .param("checkIn", "2030-06-10")
                        .param("checkOut", "2030-06-12")
                        .param("guests", "2")
                        .param("minPriceMinor", "4000")
                        .param("maxPriceMinor", "6000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/properties").param("guests", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/properties").param("checkIn", "2030-06-10"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/properties").param("minPriceMinor", "9000").param("maxPriceMinor", "8000"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/properties").param("minStarRating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/properties").param("minStarRating", "6"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/properties").param("checkIn", LocalDate.now().minusDays(1).toString())
                        .param("checkOut", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/properties").param("checkIn", "not-a-date"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/properties").param("amenities", "SPA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(propertyId));

        String bookingRequest = bookingRequest(propertyId, familyRoomTypeId);
        MvcResult firstBookingResult = createBooking(bookingRequest);
        String bookingId = readId(firstBookingResult.getResponse().getContentAsString(), "id");
        mockMvc.perform(get("/api/bookings/{bookingId}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
        mockMvc.perform(post("/api/bookings/{bookingId}/payments", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("payment-card.json")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/bookings/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/properties")
                        .param("checkIn", "2030-06-10")
                        .param("checkOut", "2030-06-12")
                        .param("guests", "2")
                        .param("minPriceMinor", "11000")
                        .param("maxPriceMinor", "13000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isConflict());

        MvcResult cardPayment = pay(bookingId, "payment-card.json", "card-payment-1");
        String transactionId = readId(cardPayment.getResponse().getContentAsString(), "transactionId");
        assertThat(cardPayment.getResponse().getContentAsString()).contains("CONFIRMED");

        MvcResult repeatedPayment = pay(bookingId, "payment-card.json", "card-payment-1");
        assertThat(readId(repeatedPayment.getResponse().getContentAsString(), "transactionId"))
                .isEqualTo(transactionId);
        mockMvc.perform(post("/api/bookings/{bookingId}/payments", bookingId)
                        .header("Idempotency-Key", "card-payment-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("payment-upi.json")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/bookings/{bookingId}/cancellation", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmountMinor").value(24000))
                .andExpect(jsonPath("$.booking.status").value("CANCELLED"));
        mockMvc.perform(post("/api/bookings/{bookingId}/cancellation", bookingId))
                .andExpect(status().isConflict());

        MvcResult reusedKeyBooking = createBooking(bookingRequest);
        String reusedKeyBookingId = readId(reusedKeyBooking.getResponse().getContentAsString(), "id");
        mockMvc.perform(post("/api/bookings/{bookingId}/payments", reusedKeyBookingId)
                        .header("Idempotency-Key", "card-payment-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("payment-card.json")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/bookings/{bookingId}/cancellation", reusedKeyBookingId))
                .andExpect(status().isOk());

        assertPaymentMethodWorks(bookingRequest, "payment-upi.json", "upi-payment-2");
        assertPaymentMethodWorks(bookingRequest, "payment-wallet.json", "wallet-payment-3");

        MvcResult pendingBooking = createBooking(bookingRequest);
        String pendingBookingId = readId(pendingBooking.getResponse().getContentAsString(), "id");
        mockMvc.perform(post("/api/bookings/{bookingId}/cancellation", pendingBookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundAmountMinor").value(0));
        mockMvc.perform(post("/api/bookings/{bookingId}/payments", pendingBookingId)
                        .header("Idempotency-Key", "cancelled-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("payment-card.json")))
                .andExpect(status().isConflict());
    }

    private MvcResult createBooking(String requestBody) throws Exception {
        return mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult pay(String bookingId, String requestFile, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/bookings/{bookingId}/payments", bookingId)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(requestFile)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void assertPaymentMethodWorks(String bookingRequest, String paymentRequestFile,
                                          String idempotencyKey) throws Exception {
        MvcResult bookingResult = createBooking(bookingRequest);
        String bookingId = readId(bookingResult.getResponse().getContentAsString(), "id");
        pay(bookingId, paymentRequestFile, idempotencyKey);
        mockMvc.perform(post("/api/bookings/{bookingId}/cancellation", bookingId))
                .andExpect(status().isOk());
    }

    private String bookingRequest(String propertyId, String roomTypeId) throws IOException {
        return requestJson("booking.json").formatted(propertyId, roomTypeId);
    }

    private String requestJson(String fileName) throws IOException {
        try (InputStream requestFile = getClass().getResourceAsStream("/requests/" + fileName)) {
            assertThat(requestFile).as("Request fixture %s exists", fileName).isNotNull();
            return new String(requestFile.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readId(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\\\"" + fieldName + "\\\":\\\"([^\\\"]+)\\\"").matcher(json);
        assertThat(matcher.find()).as("JSON field %s", fieldName).isTrue();
        return matcher.group(1);
    }

    private String readNamedId(String json, String value) {
        Matcher nameMatcher = Pattern.compile("\\\"name\\\":\\\"" + Pattern.quote(value) + "\\\"")
                .matcher(json);
        assertThat(nameMatcher.find()).as("Name %s", value).isTrue();

        Matcher idMatcher = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"")
                .matcher(json.substring(0, nameMatcher.start()));
        String matchingId = null;
        while (idMatcher.find()) {
            matchingId = idMatcher.group(1);
        }
        assertThat(matchingId).as("ID for %s", value).isNotNull();
        return matchingId;
    }
}
