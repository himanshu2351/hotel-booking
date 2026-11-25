package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.property.PropertyPortfolio;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPortfolioRepository implements PortfolioRepository {
    private final Map<UUID, PropertyPortfolio> portfolios = new ConcurrentHashMap<>();
    public PropertyPortfolio save(PropertyPortfolio portfolio) {
        portfolios.put(portfolio.id(), portfolio);
        return portfolio;
    }

    public Optional<PropertyPortfolio> findById(UUID id) {
        return Optional.ofNullable(portfolios.get(id));
    }

    @Override
    public Optional<PropertyPortfolio> findByOwnerEmail(String ownerEmail) {
        String normalizedEmail = normalizeEmail(ownerEmail);
        return portfolios.values().stream()
                .filter(portfolio -> normalizeEmail(portfolio.ownerEmail()).equals(normalizedEmail))
                .findFirst();
    }

    public List<PropertyPortfolio> findAll() {
        return List.copyOf(portfolios.values());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
