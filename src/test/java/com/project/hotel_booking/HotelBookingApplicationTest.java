package com.project.hotel_booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;

class HotelBookingApplicationTest {
    @Test
    void mainPassesArgumentsToSpringApplication() {
        String[] arguments = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            HotelBookingApplication.main(arguments);

            springApplication.verify(() -> SpringApplication.run(HotelBookingApplication.class, arguments));
        }
    }
}
