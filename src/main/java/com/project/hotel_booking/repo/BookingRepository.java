package com.project.hotel_booking.repo;

import com.project.hotel_booking.helper.booking.Booking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(UUID id);
    List<Booking> findAll();
}
