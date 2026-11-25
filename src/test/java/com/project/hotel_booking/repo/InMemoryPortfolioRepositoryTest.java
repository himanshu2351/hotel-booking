package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.property.PropertyPortfolio;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryPortfolioRepositoryTest {
    private final InMemoryPortfolioRepository repository = new InMemoryPortfolioRepository();

    @Test
    void savesFindsAndListsPortfolios() {
        PropertyPortfolio portfolio = new PropertyPortfolio(UUID.randomUUID(), "Asha", "asha@example.com", List.of());

        assertThat(repository.save(portfolio)).isSameAs(portfolio);
        assertThat(repository.findById(portfolio.id())).contains(portfolio);
        assertThat(repository.findAll()).containsExactly(portfolio);
    }

    @Test
    void returnsEmptyResultsForUnknownPortfolios() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findsOwnerEmailIgnoringCaseAndSurroundingWhitespace() {
        PropertyPortfolio portfolio = new PropertyPortfolio(
                UUID.randomUUID(), "Asha", "asha@example.com", List.of());
        repository.save(portfolio);

        assertThat(repository.findByOwnerEmail("  ASHA@EXAMPLE.COM ")).contains(portfolio);
        assertThat(repository.findByOwnerEmail("other@example.com")).isEmpty();
    }

    @Test
    void returnsEmptyWhenSearchingWithANullEmail() {
        assertThat(repository.findByOwnerEmail(null)).isEmpty();
    }
}
