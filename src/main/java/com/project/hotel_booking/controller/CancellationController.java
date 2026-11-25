package com.project.hotel_booking.controller;

import com.project.hotel_booking.helper.cancellation.CancellationOutcome;
import com.project.hotel_booking.service.CancellationOperations;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings/{bookingId}/cancellation")
public class CancellationController {
    private final CancellationOperations cancellationService;
    public CancellationController(CancellationOperations cancellationService) {
        this.cancellationService = cancellationService;
    }

    @PostMapping
    public CancellationOutcome cancel(@PathVariable UUID bookingId) {
        return cancellationService.cancel(bookingId);
    }
}
