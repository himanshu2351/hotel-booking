package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.property.PropertyPortfolio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {
    PropertyPortfolio save(PropertyPortfolio portfolio);
    Optional<PropertyPortfolio> findById(UUID id);
    Optional<PropertyPortfolio> findByOwnerEmail(String ownerEmail);
    List<PropertyPortfolio> findAll();
}
