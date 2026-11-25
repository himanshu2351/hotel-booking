package com.project.hotel_booking.service.impl;

import com.project.hotel_booking.service.CancellationOperations;
import com.project.hotel_booking.exception.ConflictException;
import com.project.hotel_booking.exception.NotFoundException;
import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.cancellation.CancellationPolicy;
import com.project.hotel_booking.helper.cancellation.CancellationOutcome;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.utils.InventoryLockManager;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CancellationService implements CancellationOperations {
    private final BookingRepository bookings;
    private final CancellationPolicy cancellationPolicy;
    private final InventoryLockManager inventoryLockManager;

    public CancellationService(BookingRepository bookings, CancellationPolicy cancellationPolicy,
                               InventoryLockManager inventoryLockManager) {
        this.bookings = bookings;
        this.cancellationPolicy = cancellationPolicy;
        this.inventoryLockManager = inventoryLockManager;
    }

    @Override
    public CancellationOutcome cancel(UUID bookingId) {
        Booking initialBooking = findBooking(bookingId);
        return inventoryLockManager.withLock(initialBooking.propertyId(), initialBooking.roomTypeId(), () -> {
            Booking booking = findBooking(bookingId);
            if (booking.status() != BookingStatus.CONFIRMED && booking.status() != BookingStatus.PENDING_PAYMENT) {
                throw new ConflictException("Only pending or confirmed bookings can be cancelled");
            }
            long refund = booking.status() == BookingStatus.CONFIRMED
                    ? cancellationPolicy.refundAmountMinor(booking)
                    : 0;
            Booking cancelled = booking.withStatus(BookingStatus.CANCELLED);
            bookings.save(cancelled);
            return new CancellationOutcome(cancelled, refund);
        });
    }

    private Booking findBooking(UUID bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

}
