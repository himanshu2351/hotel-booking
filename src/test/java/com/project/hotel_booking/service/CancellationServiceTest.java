package com.project.hotel_booking.service;

import com.project.hotel_booking.service.impl.CancellationService;

import com.project.hotel_booking.exception.ConflictException;
import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.cancellation.CancellationPolicy;
import com.project.hotel_booking.repo.BookingRepository;
import com.project.hotel_booking.utils.InventoryLockManager;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancellationServiceTest {
    @Test
    void cancelsConfirmedBookingAndReturnsPolicyRefund() {
        Booking booking = booking(BookingStatus.CONFIRMED);
        BookingRepository bookings = mock(BookingRepository.class);
        CancellationPolicy policy = mock(CancellationPolicy.class);
        when(bookings.findById(booking.id())).thenReturn(Optional.of(booking));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policy.refundAmountMinor(booking)).thenReturn(12000L);
        CancellationService service = new CancellationService(bookings, policy, new InventoryLockManager());

        var outcome = service.cancel(booking.id());

        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(outcome.refundAmountMinor()).isEqualTo(12000L);
    }

    @Test
    void cancelsPendingBookingWithoutRefund() {
        Booking booking = booking(BookingStatus.PENDING_PAYMENT);
        BookingRepository bookings = mock(BookingRepository.class);
        when(bookings.findById(booking.id())).thenReturn(Optional.of(booking));
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var outcome = new CancellationService(bookings, mock(CancellationPolicy.class), new InventoryLockManager())
                .cancel(booking.id());

        assertThat(outcome.refundAmountMinor()).isZero();
        assertThat(outcome.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookings).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBookingThatIsAlreadyCancelled() {
        Booking booking = booking(BookingStatus.CANCELLED);
        BookingRepository bookings = mock(BookingRepository.class);
        when(bookings.findById(booking.id())).thenReturn(Optional.of(booking));
        CancellationService service = new CancellationService(bookings, mock(CancellationPolicy.class),
                new InventoryLockManager());

        assertThatThrownBy(() -> service.cancel(booking.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsPaymentFailedBookingsAndDoesNotSaveThemAsCancelled() {
        Booking booking = booking(BookingStatus.PAYMENT_FAILED);
        BookingRepository bookings = mock(BookingRepository.class);
        when(bookings.findById(booking.id())).thenReturn(Optional.of(booking));
        CancellationService service = new CancellationService(bookings, mock(CancellationPolicy.class),
                new InventoryLockManager());

        assertThatThrownBy(() -> service.cancel(booking.id())).isInstanceOf(ConflictException.class);
        verify(bookings, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void leavesBookingUnchangedWhenCancellationPolicyThrows() {
        Booking booking = booking(BookingStatus.CONFIRMED);
        BookingRepository bookings = mock(BookingRepository.class);
        CancellationPolicy policy = mock(CancellationPolicy.class);
        when(bookings.findById(booking.id())).thenReturn(Optional.of(booking));
        when(policy.refundAmountMinor(booking)).thenThrow(new IllegalStateException("Policy unavailable"));
        CancellationService service = new CancellationService(bookings, policy, new InventoryLockManager());

        assertThatThrownBy(() -> service.cancel(booking.id())).isInstanceOf(IllegalStateException.class);
        verify(bookings, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Booking booking(BookingStatus status) {
        return new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Guest", "guest@example.com",
                LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 3), 2, 1, 12000L, status, Instant.now());
    }
}
