package com.project.hotel_booking.service;

import com.project.hotel_booking.service.impl.BookingService;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.exception.ConflictException;
import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingRequest;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.pricing.PricingStrategy;
import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.repo.PortfolioRepository;
import com.project.hotel_booking.utils.InventoryAvailability;
import com.project.hotel_booking.utils.InventoryLockManager;
import com.project.hotel_booking.utils.validation.BookingValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingServiceTest {
    @Test
    void createsPendingBookingWithPriceFromStrategy() {
        UUID propertyId = UUID.randomUUID();
        HotelProperty.RoomType room = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 2, 10000L);
        HotelProperty property = new HotelProperty(propertyId, UUID.randomUUID(), "Harbor Inn", "Mumbai",
                "Colaba", "Address", 4, java.util.Set.of(), List.of(room));
        PortfolioRepository portfolios = mock(PortfolioRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        InventoryAvailability availability = mock(InventoryAvailability.class);
        PricingStrategy pricing = mock(PricingStrategy.class);
        BookingRequest request = new BookingRequest(propertyId, room.id(), "Guest", "guest@example.com",
                LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3), 2, 1);
        when(portfolios.findAll()).thenReturn(List.of(new PropertyPortfolio(UUID.randomUUID(), "Owner",
                "owner@example.com", List.of(property))));
        when(bookings.findAll()).thenReturn(List.of());
        when(availability.availableRooms(propertyId, room, request.checkIn(), request.checkOut(), List.of())).thenReturn(2L);
        when(pricing.totalPriceMinor(property, room, request)).thenReturn(20000L);
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BookingService service = new BookingService(portfolios, bookings, availability, new InventoryLockManager(),
                new BookingValidator(), pricing);

        Booking booking = service.createBooking(request);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.totalPriceMinor()).isEqualTo(20000L);
        org.mockito.Mockito.verify(bookings).save(booking);
    }

    @Test
    void rejectsBookingWhenAvailabilityIsInsufficient() {
        HotelProperty.RoomType room = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 1, 10000L);
        HotelProperty property = new HotelProperty(UUID.randomUUID(), UUID.randomUUID(), "Harbor Inn", "Mumbai",
                "Colaba", "Address", 4, java.util.Set.of(), List.of(room));
        PortfolioRepository portfolios = mock(PortfolioRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        InventoryAvailability availability = mock(InventoryAvailability.class);
        BookingRequest request = new BookingRequest(property.id(), room.id(), "Guest", "guest@example.com",
                LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3), 2, 1);
        when(portfolios.findAll()).thenReturn(List.of(new PropertyPortfolio(UUID.randomUUID(), "Owner",
                "owner@example.com", List.of(property))));
        when(bookings.findAll()).thenReturn(List.of());
        when(availability.availableRooms(any(), any(), any(), any(), any())).thenReturn(0L);
        BookingService service = new BookingService(portfolios, bookings, availability, new InventoryLockManager(),
                new BookingValidator(), mock(PricingStrategy.class));

        assertThatThrownBy(() -> service.createBooking(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsBookingWhenTheCalculatedTotalOverflows() {
        UUID propertyId = UUID.randomUUID();
        HotelProperty.RoomType room = new HotelProperty.RoomType(UUID.randomUUID(), "Queen", 2, 2, Long.MAX_VALUE);
        HotelProperty property = new HotelProperty(propertyId, UUID.randomUUID(), "Harbor Inn", "Mumbai",
                "Colaba", "Address", 4, java.util.Set.of(), List.of(room));
        PortfolioRepository portfolios = mock(PortfolioRepository.class);
        BookingRepository bookings = mock(BookingRepository.class);
        InventoryAvailability availability = mock(InventoryAvailability.class);
        PricingStrategy pricing = mock(PricingStrategy.class);
        BookingRequest request = new BookingRequest(propertyId, room.id(), "Guest", "guest@example.com",
                LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3), 2, 1);
        when(portfolios.findAll()).thenReturn(List.of(new PropertyPortfolio(UUID.randomUUID(), "Owner",
                "owner@example.com", List.of(property))));
        when(bookings.findAll()).thenReturn(List.of());
        when(availability.availableRooms(any(), any(), any(), any(), any())).thenReturn(2L);
        when(pricing.totalPriceMinor(property, room, request)).thenThrow(new ArithmeticException("overflow"));
        BookingService service = new BookingService(portfolios, bookings, availability, new InventoryLockManager(),
                new BookingValidator(), pricing);

        assertThatThrownBy(() -> service.createBooking(request)).isInstanceOf(BadRequestException.class);
        org.mockito.Mockito.verify(bookings, org.mockito.Mockito.never()).save(any());
    }
}
