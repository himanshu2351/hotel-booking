package com.project.hotel_booking.service.impl;

import com.project.hotel_booking.service.BookingOperations;
import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.exception.ConflictException;
import com.project.hotel_booking.exception.NotFoundException;
import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingRequest;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.helper.pricing.PricingStrategy;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.repo.PortfolioRepository;
import com.project.hotel_booking.utils.InventoryAvailability;
import com.project.hotel_booking.utils.InventoryLockManager;
import com.project.hotel_booking.utils.validation.BookingValidator;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class BookingService implements BookingOperations {
    private final PortfolioRepository portfolios;
    private final BookingRepository bookings;
    private final InventoryAvailability inventoryAvailability;
    private final InventoryLockManager inventoryLockManager;
    private final BookingValidator validator;
    private final PricingStrategy pricingStrategy;

    public BookingService(PortfolioRepository portfolios, BookingRepository bookings,
                         InventoryAvailability inventoryAvailability, InventoryLockManager inventoryLockManager,
                         BookingValidator validator, PricingStrategy pricingStrategy) {
        this.portfolios = portfolios;
        this.bookings = bookings;
        this.inventoryAvailability = inventoryAvailability;
        this.inventoryLockManager = inventoryLockManager;
        this.validator = validator;
        this.pricingStrategy = pricingStrategy;
    }

    @Override
    public Booking createBooking(BookingRequest request) {
        validator.validate(request);
        HotelProperty property = findProperty(request.propertyId());
        HotelProperty.RoomType room = property.roomTypes().stream()
                .filter(candidate -> candidate.id().equals(request.roomTypeId()))
                .findFirst().orElseThrow(() -> new NotFoundException("Room type not found for property"));
        validator.validateGuestCapacity(room.maxGuests(), request.rooms(), request.guests());

        return inventoryLockManager.withLock(property.id(), room.id(), () -> {
            if (inventoryAvailability.availableRooms(
                    property.id(), room, request.checkIn(), request.checkOut(), bookings.findAll()
            ) < request.rooms()) {
                throw new ConflictException("Not enough rooms are available for those dates");
            }
            long total;
            try {
                total = pricingStrategy.totalPriceMinor(property, room, request);
            } catch (ArithmeticException exception) {
                throw new BadRequestException("Booking price exceeds the supported amount");
            }
            Booking booking = new Booking(
                    UUID.randomUUID(),
                    property.id(),
                    room.id(),
                    request.guestName(),
                    request.guestEmail(),
                    request.checkIn(),
                    request.checkOut(),
                    request.guests(),
                    request.rooms(),
                    total,
                    BookingStatus.PENDING_PAYMENT,
                    java.time.Instant.now()
            );
            return bookings.save(booking);
        });
    }

    @Override
    public Booking booking(UUID id) {
        return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private HotelProperty findProperty(UUID id) {
        return portfolios.findAll().stream().flatMap(portfolio -> portfolio.properties().stream())
                .filter(property -> property.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Property not found"));
    }
}
