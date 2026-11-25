package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingRequest;
import com.project.hotel_booking.service.BookingOperations;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingOperations bookingService;
    public BookingController(BookingOperations bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public Booking book(@RequestBody BookingRequest request) {
        return bookingService.createBooking(request);
    }
    @GetMapping("/{bookingId}")
    public Booking booking(@PathVariable UUID bookingId) {
        return bookingService.booking(bookingId);
    }
}
