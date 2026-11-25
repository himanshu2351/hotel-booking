package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.booking.Booking;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {
    private final Map<UUID, Booking> bookings = new ConcurrentHashMap<>();
    public Booking save(Booking booking) {
        bookings.put(booking.id(), booking);
        return booking;
    }

    public Optional<Booking> findById(UUID id) {
        return Optional.ofNullable(bookings.get(id));
    }

    public List<Booking> findAll() {
        return List.copyOf(bookings.values());
    }
}
