package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.service.PropertyOnboardingOperations;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PropertyOnboardingController {
    private final PropertyOnboardingOperations onboardingService;
    public PropertyOnboardingController(PropertyOnboardingOperations onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/portfolios")
    public PropertyPortfolio createPortfolio(@RequestBody OwnerDetails owner) {
        return onboardingService.createPortfolio(owner);
    }

    @GetMapping("/portfolios")
    public List<PropertyPortfolio> portfolios() {
        return onboardingService.portfolios();
    }

    @GetMapping("/portfolios/{portfolioId}")
    public PropertyPortfolio portfolio(@PathVariable UUID portfolioId) {
        return onboardingService.portfolio(portfolioId);
    }

    @PostMapping("/portfolios/{portfolioId}/properties")
    public PropertyPortfolio addProperty(@PathVariable UUID portfolioId, @RequestBody PropertyDetails details) {
        return onboardingService.addProperty(portfolioId, details);
    }

}
