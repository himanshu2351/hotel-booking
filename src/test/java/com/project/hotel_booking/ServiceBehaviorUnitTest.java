package com.project.hotel_booking;

import com.project.hotel_booking.exception.BadRequestException;
import com.project.hotel_booking.exception.ConflictException;
import com.project.hotel_booking.exception.NotFoundException;
import com.project.hotel_booking.helper.booking.Booking;
import com.project.hotel_booking.helper.booking.BookingRequest;
import com.project.hotel_booking.helper.booking.BookingStatus;
import com.project.hotel_booking.helper.cancellation.CancellationPolicy;
import com.project.hotel_booking.helper.payment.PaymentMethod;
import com.project.hotel_booking.helper.payment.PaymentOutcome;
import com.project.hotel_booking.helper.payment.PaymentProcessor;
import com.project.hotel_booking.helper.payment.PaymentRecord;
import com.project.hotel_booking.helper.payment.PaymentRequest;
import com.project.hotel_booking.helper.payment.PaymentResult;
import com.project.hotel_booking.helper.pricing.PricingStrategy;
import com.project.hotel_booking.helper.pricing.StandardPricingStrategy;
import com.project.hotel_booking.helper.property.HotelProperty;
import com.project.hotel_booking.helper.property.OwnerDetails;
import com.project.hotel_booking.helper.property.PropertyDetails;
import com.project.hotel_booking.helper.property.PropertyPortfolio;
import com.project.hotel_booking.repo.InMemoryBookingRepository;
import com.project.hotel_booking.repo.InMemoryPaymentRecordRepository;
import com.project.hotel_booking.repo.InMemoryPortfolioRepository;
import com.project.hotel_booking.service.impl.BookingService;
import com.project.hotel_booking.service.impl.CancellationService;
import com.project.hotel_booking.service.impl.PaymentService;
import com.project.hotel_booking.service.impl.PropertyOnboardingService;
import com.project.hotel_booking.utils.IdempotencyLockManager;
import com.project.hotel_booking.utils.InventoryAvailability;
import com.project.hotel_booking.utils.InventoryLockManager;
import com.project.hotel_booking.utils.validation.BookingValidator;
import com.project.hotel_booking.utils.validation.PaymentValidator;
import com.project.hotel_booking.utils.validation.PropertyOnboardingValidator;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceBehaviorUnitTest {
    private static final LocalDate CHECK_IN = LocalDate.of(2031, 7, 10);
    private static final LocalDate CHECK_OUT = LocalDate.of(2031, 7, 12);

    @Test
    void onboardingBuildsPortfolioPropertiesWithOptionalAddressAndAmenities() {
        InMemoryPortfolioRepository portfolios = new InMemoryPortfolioRepository();
        PropertyOnboardingService service = new PropertyOnboardingService(
                portfolios, new PropertyOnboardingValidator()
        );
        PropertyPortfolio emptyPortfolio = service.createPortfolio(new OwnerDetails("Owner", "owner@example.com"));
        PropertyPortfolio updatedPortfolio = service.addProperty(
                emptyPortfolio.id(),
                propertyDetails("Tiny Inn", null, null, 1)
        );

        assertThat(updatedPortfolio.properties()).hasSize(1);
        assertThat(updatedPortfolio.properties().get(0).address()).isEmpty();
        assertThat(updatedPortfolio.properties().get(0).amenities()).isEmpty();
        assertThat(service.portfolios()).containsExactly(updatedPortfolio);
        assertThat(service.portfolio(emptyPortfolio.id())).isEqualTo(updatedPortfolio);

        assertThatThrownBy(() -> service.portfolio(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.createPortfolio(null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.addProperty(UUID.randomUUID(), propertyDetails("Hotel", "", Set.of(), 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void bookingServiceRejectsMissingInventoryAndAccommodatesValidRequests() {
        InMemoryPortfolioRepository portfolios = new InMemoryPortfolioRepository();
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        InventoryLockManager lockManager = new InventoryLockManager();
        HotelProperty.RoomType roomType = roomType(1, 2, 12000L);
        HotelProperty property = property(roomType);
        portfolios.save(portfolio(property));
        BookingService service = bookingService(portfolios, bookings, lockManager, new StandardPricingStrategy());

        Booking booking = service.createBooking(bookingRequest(property, roomType, 2));
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.totalPriceMinor()).isEqualTo(24000L);
        assertThat(service.booking(booking.id())).isEqualTo(booking);

        assertThatThrownBy(() -> service.createBooking(bookingRequest(property, roomType, 2)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.booking(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);

        HotelProperty.RoomType unknownRoom = roomType(1, 2, 12000L);
        assertThatThrownBy(() -> service.createBooking(new BookingRequest(
                property.id(), unknownRoom.id(), "Guest", "guest@example.com", CHECK_IN, CHECK_OUT, 1, 1
        ))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.createBooking(new BookingRequest(
                UUID.randomUUID(), roomType.id(), "Guest", "guest@example.com", CHECK_IN, CHECK_OUT, 1, 1
        ))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.createBooking(bookingRequest(property, roomType, 3)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void concurrentBookingsForTheSameRoomTypeCannotOversellInventory() throws Exception {
        InMemoryPortfolioRepository portfolios = new InMemoryPortfolioRepository();
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        HotelProperty.RoomType roomType = roomType(1, 2, 9000L);
        HotelProperty property = property(roomType);
        portfolios.save(portfolio(property));
        BookingService service = bookingService(portfolios, bookings, new InventoryLockManager(), new StandardPricingStrategy());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> firstRequest = executor.submit(() -> attemptBooking(service, property, roomType, ready, start));
            Future<Boolean> secondRequest = executor.submit(() -> attemptBooking(service, property, roomType, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(firstRequest.get(5, TimeUnit.SECONDS), secondRequest.get(5, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(bookings.findAll()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void paymentServiceHandlesGatewayFailureRetriesAndMissingBookings() {
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        InMemoryPaymentRecordRepository paymentRecords = new InMemoryPaymentRecordRepository();
        HotelProperty.RoomType roomType = roomType(1, 2, 10000L);
        HotelProperty property = property(roomType);
        Booking pendingBooking = booking(property, roomType, BookingStatus.PENDING_PAYMENT);
        bookings.save(pendingBooking);
        PaymentProcessor declinedCard = new PaymentProcessor() {
            @Override
            public PaymentMethod method() {
                return PaymentMethod.CARD;
            }

            @Override
            public PaymentResult charge(UUID bookingId, long amountMinor, String idempotencyKey) {
                return new PaymentResult(false, "declined", "Payment declined");
            }
        };
        PaymentService service = new PaymentService(
                bookings,
                paymentRecords,
                List.of(declinedCard),
                new InventoryLockManager(),
                new IdempotencyLockManager(),
                new PaymentValidator()
        );

        PaymentOutcome firstOutcome = service.pay(
                pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), "declined-1"
        );
        assertThat(firstOutcome.booking().status()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(firstOutcome.payment().successful()).isFalse();
        assertThat(service.pay(pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), "declined-1"))
                .isEqualTo(firstOutcome);
        assertThat(paymentRecords.findByIdempotencyKey("declined-1")).isPresent();

        assertThatThrownBy(() -> service.pay(UUID.randomUUID(), new PaymentRequest(PaymentMethod.CARD), "missing-1"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.pay(pendingBooking.id(), new PaymentRequest(PaymentMethod.UPI), "unsupported-1"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.pay(pendingBooking.id(), new PaymentRequest(PaymentMethod.CARD), "declined-2"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancellationServiceRejectsMissingAndTerminalBookings() {
        InMemoryBookingRepository bookings = new InMemoryBookingRepository();
        HotelProperty.RoomType roomType = roomType(1, 2, 10000L);
        HotelProperty property = property(roomType);
        Booking failedBooking = booking(property, roomType, BookingStatus.PAYMENT_FAILED);
        bookings.save(failedBooking);
        CancellationPolicy policy = booking -> booking.totalPriceMinor();
        CancellationService service = new CancellationService(bookings, policy, new InventoryLockManager());

        assertThatThrownBy(() -> service.cancel(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cancel(failedBooking.id()))
                .isInstanceOf(ConflictException.class);
    }

    private boolean attemptBooking(BookingService service, HotelProperty property, HotelProperty.RoomType roomType,
                                   CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            service.createBooking(bookingRequest(property, roomType, 1));
            return true;
        } catch (ConflictException expected) {
            return false;
        }
    }

    private BookingService bookingService(InMemoryPortfolioRepository portfolios, InMemoryBookingRepository bookings,
                                          InventoryLockManager lockManager, PricingStrategy pricingStrategy) {
        return new BookingService(
                portfolios, bookings, new InventoryAvailability(), lockManager,
                new BookingValidator(), pricingStrategy
        );
    }

    private PropertyDetails propertyDetails(String name, String address, Set<String> amenities, int inventoryCount) {
        return new PropertyDetails(
                name, "City", "Area", address, 4, amenities,
                List.of(new PropertyDetails.RoomTypeDetails("Twin", 2, inventoryCount, 12000L))
        );
    }

    private HotelProperty.RoomType roomType(int inventoryCount, int maxGuests, long nightlyPriceMinor) {
        return new HotelProperty.RoomType(UUID.randomUUID(), "Twin", maxGuests, inventoryCount, nightlyPriceMinor);
    }

    private HotelProperty property(HotelProperty.RoomType roomType) {
        return new HotelProperty(
                UUID.randomUUID(), UUID.randomUUID(), "Hotel", "City", "Area", "", 4,
                Set.of("WIFI"), List.of(roomType)
        );
    }

    private PropertyPortfolio portfolio(HotelProperty property) {
        return new PropertyPortfolio(UUID.randomUUID(), "Owner", "owner@example.com", List.of(property));
    }

    private BookingRequest bookingRequest(HotelProperty property, HotelProperty.RoomType roomType, int guests) {
        return new BookingRequest(
                property.id(), roomType.id(), "Guest", "guest@example.com", CHECK_IN, CHECK_OUT, guests, 1
        );
    }

    private Booking booking(HotelProperty property, HotelProperty.RoomType roomType, BookingStatus status) {
        return new Booking(
                UUID.randomUUID(), property.id(), roomType.id(), "Guest", "guest@example.com",
                CHECK_IN, CHECK_OUT, 1, 1, 20000L, status, java.time.Instant.now()
        );
    }
}
