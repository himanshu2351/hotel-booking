# Test Scenarios

This document describes the situations covered by the project's unit tests and API integration test in plain English.

## Property owner and property setup

- A valid owner can create an empty portfolio.
- An owner email can be registered only once; a duplicate with different letter casing is rejected and does not create another portfolio.
- Portfolio lists and lookups return saved portfolios.
- Looking up a portfolio that does not exist is rejected.
- A property can be added to an existing portfolio, including a portfolio that previously had no properties.
- Optional property address and amenities can be omitted; the saved property uses an empty address and no amenities.
- Adding a property to a portfolio that does not exist is rejected.
- Owner details are rejected when missing or when the owner's name or email is blank.
- Owner email is rejected when it is malformed or longer than the supported limit.
- Property details are rejected when required name or location fields are blank, the star rating is outside 1 to 5, or no room types are supplied.
- Room details are rejected when the room name is blank, guest capacity or inventory is below one, or the price is negative. A zero price is accepted.
- A room list containing a null room entry, or amenities containing a null or blank value, is rejected as a bad request instead of failing during property creation.

## Property search

- City and locality searches are case-insensitive and can be omitted.
- A property must include every requested amenity.
- A minimum star rating is optional and filters out properties below the requested rating.
- A room's price is checked against minimum and maximum nightly prices, including the boundary values. The price strategy supplies the rate, and the filter accounts for how many rooms the guest count requires.
- Browse searches without dates use the room type's configured inventory. Date-based searches use the inventory available for those dates.
- A property appears only when its property filters pass and at least one individual room type passes the room filters.
- Search validation rejects a missing search request, only one of check-in or check-out, check-out on or before check-in, fewer than one guest, negative prices, and a minimum price above the maximum. Valid searches with optional or complete criteria are accepted.
- Search rejects past or same-day check-in/check-out ranges, star ratings outside 1 to 5, and null or blank amenity filters. Omitted amenities are treated as no amenity filter.
- Availability counts the greatest number of rooms occupied on any night in the requested range, using a wider total so excessive stored counts cannot wrap around into apparent availability. It counts pending-payment and confirmed bookings, and ignores cancelled or failed-payment bookings, other properties, other room types, and bookings outside the requested dates. Check-out is treated as the end of the stay, so a booking ending on a requested check-in date does not overlap it.

## Booking

- A valid request for an available room creates a booking in the pending-payment state and uses the pricing strategy's total.
- A booking can be retrieved by its ID; a missing booking is rejected.
- Booking creation is rejected when the property or room type does not exist, the requested guest count exceeds room capacity, or there is not enough inventory for the dates.
- Booking requests are rejected for missing, past, or reversed dates; missing property or room type IDs; fewer than one guest or room; a missing or blank guest name or email; malformed email; and guest capacity that is not supported by the selected rooms.
- Two simultaneous requests for the last room of the same room type cannot both succeed; only one booking is saved.
- Capacity checks use a wider integer type so large valid capacity calculations do not wrap around.
- The standard pricing strategy uses the configured nightly rate, multiplies it by the number of nights and rooms, and reports arithmetic overflow rather than silently returning a wrong total.

## Payment

- Card, UPI, and wallet mock processors report successful payments and return transaction IDs with the expected method-specific format.
- A successful payment changes a pending booking to confirmed.
- A declined payment changes the booking to payment failed. Repeating the same idempotency key and request returns the original payment result.
- A processor exception or missing processor result is treated as an unknown outcome: the booking remains pending, a `502` is returned, and a retry with the same key can complete after the processor recovers.
- Simultaneous requests with the same idempotency key, including keys that differ only by surrounding whitespace, result in one gateway charge and the same outcome for both callers.
- Registering two payment processors for the same method is rejected during service setup rather than silently replacing one processor.
- A payment can be made only for a booking that is awaiting payment. A missing booking or a booking already cancelled or otherwise no longer awaiting payment is rejected.
- An idempotency key is trimmed before use. Missing, blank, or overlong keys are rejected.
- A missing payment request or method, and a method not supported by the configured processors, are rejected.
- Reusing an idempotency key for a different method or a different booking is rejected as a conflict.

## Cancellation

- A confirmed booking can be cancelled, and the configured full-refund policy returns the booking's paid amount.
- A pending-payment booking can be cancelled with no refund.
- Cancelling a booking that is already cancelled, or otherwise in a terminal state, is rejected.
- Cancelling a payment-failed booking is rejected. If the cancellation policy itself fails, the booking remains unchanged.
- Cancelling a missing booking is rejected.
- After cancellation, the booking state is recorded as cancelled and its inventory no longer counts as occupied in availability calculations.

## Components and API behavior

- Each controller is checked to make sure it passes the request values to the matching service interface and returns the service result.
- The in-memory booking, owner-portfolio, and payment-record repositories are checked for save, lookup, list where supported, and missing-record behavior.
- The API maps bad requests, missing records, and conflicts to problem responses with the corresponding HTTP status and message.
- The HTTP integration test exercises owner creation, property addition, portfolio reads, property search, booking, payment, idempotent payment retries, and cancellation through the REST endpoints.
- The HTTP test also checks invalid owner/property/search requests, missing portfolio and booking lookups, unavailable inventory, search criteria matching, and that the Swagger-backed API document endpoint responds.
- Malformed JSON, invalid UUID/date query parameters, out-of-range search star ratings, and a missing payment idempotency header receive a `400` response.
- Lock tests check that different room-type inventory keys can proceed independently. A workflow concurrency test holds payment while cancellation waits on the same inventory lock, then verifies that payment confirms before cancellation cancels and calculates its refund.
- Payment request bodies used by the HTTP tests are stored as JSON files in `src/test/resources/requests`.
- The application entry point is checked to make sure its command-line arguments are passed to Spring Boot.

## Running the tests

Run all tests and enforce the configured 100% instruction, branch, and line coverage thresholds with:

```bash
./mvnw clean verify
```

