package com.project.hotel_booking.service;

import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import com.project.hotel_booking.helper.property.PropertyPortfolio;

import java.util.List;
import java.util.UUID;

/** Application boundary for onboarding owners and their properties. */
public interface PropertyOnboardingOperations {
    PropertyPortfolio createPortfolio(OwnerDetails owner);

    List<PropertyPortfolio> portfolios();

    PropertyPortfolio portfolio(UUID id);

    PropertyPortfolio addProperty(UUID portfolioId, PropertyDetails details);
}
