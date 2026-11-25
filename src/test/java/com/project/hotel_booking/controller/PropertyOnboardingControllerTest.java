package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.service.PropertyOnboardingOperations;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyOnboardingControllerTest {
    private final PropertyOnboardingOperations onboardingService = mock(PropertyOnboardingOperations.class);
    private final PropertyOnboardingController controller = new PropertyOnboardingController(onboardingService);

    @Test
    void delegatesPortfolioCreationAndListing() {
        OwnerDetails owner = new OwnerDetails("Asha", "asha@example.com");
        PropertyPortfolio portfolio = mock(PropertyPortfolio.class);
        when(onboardingService.createPortfolio(owner)).thenReturn(portfolio);
        when(onboardingService.portfolios()).thenReturn(List.of(portfolio));

        assertThat(controller.createPortfolio(owner)).isSameAs(portfolio);
        assertThat(controller.portfolios()).containsExactly(portfolio);
        verify(onboardingService).createPortfolio(owner);
        verify(onboardingService).portfolios();
    }

    @Test
    void delegatesPortfolioLookupAndPropertyAddition() {
        UUID portfolioId = UUID.randomUUID();
        PropertyDetails details = mock(PropertyDetails.class);
        PropertyPortfolio portfolio = mock(PropertyPortfolio.class);
        when(onboardingService.portfolio(portfolioId)).thenReturn(portfolio);
        when(onboardingService.addProperty(portfolioId, details)).thenReturn(portfolio);

        assertThat(controller.portfolio(portfolioId)).isSameAs(portfolio);
        assertThat(controller.addProperty(portfolioId, details)).isSameAs(portfolio);
        verify(onboardingService).portfolio(portfolioId);
        verify(onboardingService).addProperty(portfolioId, details);
    }
}
