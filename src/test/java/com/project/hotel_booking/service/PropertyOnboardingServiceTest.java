package com.project.hotel_booking.service;

import com.project.hotel_booking.service.impl.PropertyOnboardingService;

import com.project.hotel_booking.exception.NotFoundException;
import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.repo.PortfolioRepository;
import com.project.hotel_booking.repo.InMemoryPortfolioRepository;
import com.project.hotel_booking.utils.validation.PropertyOnboardingValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PropertyOnboardingServiceTest {
    private final PortfolioRepository portfolios = mock(PortfolioRepository.class);
    private final PropertyOnboardingService service = new PropertyOnboardingService(
            portfolios, new PropertyOnboardingValidator());

    @Test
    void createsPortfolioAndListsPortfolios() {
        OwnerDetails owner = new OwnerDetails("Asha Mehta", "asha@example.com");
        when(portfolios.findByOwnerEmail(owner.ownerEmail())).thenReturn(java.util.Optional.empty());
        when(portfolios.save(any(PropertyPortfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(portfolios.findAll()).thenReturn(List.of());

        PropertyPortfolio created = service.createPortfolio(owner);

        assertThat(created.ownerName()).isEqualTo("Asha Mehta");
        assertThat(created.ownerEmail()).isEqualTo("asha@example.com");
        assertThat(created.properties()).isEmpty();
        assertThat(service.portfolios()).isEmpty();
    }

    @Test
    void addsAPropertyToExistingPortfolioAndLooksUpThePortfolio() {
        UUID portfolioId = UUID.randomUUID();
        PropertyPortfolio portfolio = new PropertyPortfolio(portfolioId, "Asha", "asha@example.com", List.of());
        when(portfolios.findById(portfolioId)).thenReturn(java.util.Optional.of(portfolio));
        when(portfolios.save(any(PropertyPortfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PropertyDetails details = new PropertyDetails("Harbor Inn", "Mumbai", "Colaba", null, 4,
                null, List.of(new PropertyDetails.RoomTypeDetails("Queen", 2, 3, 10000L)));

        PropertyPortfolio updated = service.addProperty(portfolioId, details);

        assertThat(service.portfolio(portfolioId)).isEqualTo(portfolio);
        assertThat(updated.properties()).hasSize(1);
        assertThat(updated.properties().get(0).portfolioId()).isEqualTo(portfolioId);
        assertThat(updated.properties().get(0).address()).isEmpty();
        assertThat(updated.properties().get(0).amenities()).isEmpty();
    }

    @Test
    void failsWhenAddingPropertyToAnUnknownPortfolio() {
        UUID portfolioId = UUID.randomUUID();
        when(portfolios.findById(portfolioId)).thenReturn(java.util.Optional.empty());
        PropertyDetails details = new PropertyDetails("Harbor Inn", "Mumbai", "Colaba", null, 4,
                Set.of(), List.of(new PropertyDetails.RoomTypeDetails("Queen", 2, 3, 10000L)));

        assertThatThrownBy(() -> service.addProperty(portfolioId, details)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsDuplicateOwnerEmailWithoutCaseSensitivity() {
        InMemoryPortfolioRepository portfolioRepository = new InMemoryPortfolioRepository();
        PropertyOnboardingService onboardingService = new PropertyOnboardingService(
                portfolioRepository, new PropertyOnboardingValidator());
        onboardingService.createPortfolio(new OwnerDetails("Asha Mehta", "asha@example.com"));

        assertThatThrownBy(() -> onboardingService.createPortfolio(
                new OwnerDetails("Asha Duplicate", "ASHA@EXAMPLE.COM")))
                .isInstanceOf(com.project.hotel_booking.exception.ConflictException.class)
                .hasMessage("An owner with this email already exists");
        assertThat(portfolioRepository.findAll()).hasSize(1);
    }
}
