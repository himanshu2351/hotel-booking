package com.project.hotel_booking.service.impl;

import com.project.hotel_booking.service.PropertyOnboardingOperations;
import com.project.hotel_booking.exception.ConflictException;
import com.project.hotel_booking.exception.NotFoundException;
import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.repo.PortfolioRepository;
import com.project.hotel_booking.utils.validation.PropertyOnboardingValidator;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PropertyOnboardingService implements PropertyOnboardingOperations {
    private final PortfolioRepository portfolios;
    private final PropertyOnboardingValidator validator;

    public PropertyOnboardingService(PortfolioRepository portfolios, PropertyOnboardingValidator validator) {
        this.portfolios = portfolios;
        this.validator = validator;
    }

    @Override
    public synchronized PropertyPortfolio createPortfolio(OwnerDetails owner) {
        validator.validateOwner(owner);
        if (portfolios.findByOwnerEmail(owner.ownerEmail()).isPresent()) {
            throw new ConflictException("An owner with this email already exists");
        }

        return portfolios.save(new PropertyPortfolio(
                UUID.randomUUID(),
                owner.ownerName(),
                owner.ownerEmail(),
                List.of()
        ));
    }

    @Override
    public synchronized PropertyPortfolio addProperty(UUID portfolioId, PropertyDetails details) {
        validator.validateProperty(details);
        PropertyPortfolio portfolio = portfolios.findById(portfolioId)
                .orElseThrow(() -> new NotFoundException("Owner portfolio not found"));
        HotelProperty property = makeProperty(portfolioId, details);
        List<HotelProperty> allProperties = new ArrayList<>(portfolio.properties());
        allProperties.add(property);
        return portfolios.save(new PropertyPortfolio(
                portfolio.id(),
                portfolio.ownerName(),
                portfolio.ownerEmail(),
                allProperties
        ));
    }

    @Override
    public List<PropertyPortfolio> portfolios() {
        return portfolios.findAll();
    }

    @Override
    public PropertyPortfolio portfolio(UUID id) {
        return portfolios.findById(id)
                .orElseThrow(() -> new NotFoundException("Owner portfolio not found"));
    }

    private HotelProperty makeProperty(UUID portfolioId, PropertyDetails details) {
        List<HotelProperty.RoomType> rooms = details.roomTypes().stream()
                .map(room -> new HotelProperty.RoomType(
                        UUID.randomUUID(),
                        room.name(),
                        room.maxGuests(),
                        room.inventoryCount(),
                        room.nightlyPriceMinor()
                ))
                .toList();
        return new HotelProperty(UUID.randomUUID(), portfolioId, details.name(), details.city(), details.locality(),
                details.address() == null ? "" : details.address(), details.starRating(),
                details.amenities() == null ? Set.of() : details.amenities(), rooms);
    }
}
