package com.project.hotel_booking.helper.cancellation;

import com.project.hotel_booking.helper.booking.Booking;
import org.springframework.stereotype.Component;

/** Simple default policy; replaceable with time-based or property-specific rules. */
@Component
public class FullRefundCancellationPolicy implements CancellationPolicy {
    public long refundAmountMinor(Booking booking) {
        return booking.totalPriceMinor();
    }
}
