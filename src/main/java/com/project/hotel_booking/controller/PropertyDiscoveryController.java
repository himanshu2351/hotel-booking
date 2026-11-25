package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.search.PropertySearchCriteria;
import com.project.hotel_booking.service.PropertyDiscoveryOperations;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/properties")
public class PropertyDiscoveryController {
    private final PropertyDiscoveryOperations discoveryService;
    public PropertyDiscoveryController(PropertyDiscoveryOperations discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping
    public List<HotelProperty> search(@RequestParam(required = false) String city,
                                      @RequestParam(required = false) String locality,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                                      @RequestParam(defaultValue = "1") int guests,
                                      @RequestParam(required = false) Long minPriceMinor,
                                      @RequestParam(required = false) Long maxPriceMinor,
                                      @RequestParam(required = false) Integer minStarRating,
                                      @RequestParam(required = false) Set<String> amenities) {
        PropertySearchCriteria criteria = new PropertySearchCriteria(city, locality, checkIn, checkOut, guests,
                minPriceMinor, maxPriceMinor, minStarRating, amenities == null ? Set.of() : amenities);
        return discoveryService.search(criteria);
    }
}
