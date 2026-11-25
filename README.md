# Hotel Booking Platform

A small Java 17 and Spring Boot backend for property discovery, owner onboarding, booking, payment, and cancellation. The implementation keeps business rules in a service layer and uses repository, payment processor, search filter, and cancellation policy interfaces so those parts can be replaced independently.

## Prerequisites

- JDK 17 (the Maven wrapper downloads Maven, but it does not install Java)
- A terminal with `curl` for the API examples below

No database or external payment gateway is required for the sample application. It uses in-memory repositories and mock payment processors. Data is lost when the application stops.

## Project structure

- `controller`: REST endpoints and HTTP error responses
- `service`: `*Operations` contracts used by controllers
- `service.impl`: focused discovery, onboarding, booking, payment, and cancellation service implementations
- `repo`: repository interfaces and in-memory implementations
- `utils`: inventory availability calculation, per-property-room-type locking, and `utils.validation` request validators
- `helper.booking`: booking model, lifecycle, and request
- `helper.property`: property and owner portfolio models
- `helper.search`: search criteria and pluggable filters
- `helper.payment`: payment request, result, and processor adapters
- `helper.cancellation`: cancellation policy implementations
- `helper.pricing`: nightly-rate and total-price strategy
- `exception`: application exceptions and centralized HTTP error mapping

## Build and run

Run these commands from the project root. The Maven wrapper uses the Maven version configured in `.mvn/wrapper/maven-wrapper.properties`.

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

On Windows Command Prompt or PowerShell:

```bat
mvnw.cmd spring-boot:run
```

The application listens on port `8080` by default. The API index is at `http://localhost:8080/api`, Swagger UI is at `http://localhost:8080/swagger-ui.html`, and the generated OpenAPI document is at `http://localhost:8080/v3/api-docs`.

Build a package with `./mvnw package` (Windows: `mvnw.cmd package`). Run the unit and API integration tests with `./mvnw test` (Windows: `mvnw.cmd test`).

## End-to-end API flow

The examples use `curl`. Start the application first, then use the IDs returned by each request in the next request. Choose check-in and check-out dates in the future using `YYYY-MM-DD`; check-out is exclusive, so a two-night stay from December 1 through December 3 has two nights.

### 1. Create an owner portfolio

```bash
curl -X POST http://localhost:8080/api/portfolios \
  -H 'Content-Type: application/json' \
  -d '{"ownerName":"Asha Mehta","ownerEmail":"asha@example.com"}'
```

Copy the returned portfolio `id` for the next request.

### 2. Add a property and room type

Replace `<PORTFOLIO_ID>` with the ID from step 1.

```bash
curl -X POST http://localhost:8080/api/portfolios/<PORTFOLIO_ID>/properties \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Harbor View Hotel",
    "city":"Mumbai",
    "locality":"Colaba",
    "address":"Example address",
    "starRating":4,
    "amenities":["WIFI","POOL"],
    "roomTypes":[
      {"name":"Deluxe","maxGuests":2,"inventoryCount":8,"nightlyPriceMinor":125000}
    ]
  }'
```

Copy the returned property `id` and its room type `id`. `nightlyPriceMinor` is a nightly price in integer minor currency units (for example, paise); this sample does not assign a currency or calculate tax.

### 3. Search for available properties (optional)

Replace the dates with a valid future stay. Search accepts optional city, locality, date, guest, price, star rating, and repeated amenity filters.

```bash
curl 'http://localhost:8080/api/properties?city=Mumbai&checkIn=2026-12-01&checkOut=2026-12-03&guests=2&amenities=WIFI'
```

A date range is optional when browsing, but if supplied both dates are required. Room price and availability criteria must match the same room type for a property to appear.

### 4. Create a booking

Replace `<PROPERTY_ID>`, `<ROOM_TYPE_ID>`, and the dates. A booking temporarily holds the requested room inventory while awaiting payment.

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H 'Content-Type: application/json' \
  -d '{
    "propertyId":"<PROPERTY_ID>",
    "roomTypeId":"<ROOM_TYPE_ID>",
    "guestName":"Ravi Kumar",
    "guestEmail":"ravi@example.com",
    "checkIn":"2026-12-01",
    "checkOut":"2026-12-03",
    "guests":2,
    "rooms":1
  }'
```

Copy the returned booking `id`. The response includes the calculated total and starts in `PENDING_PAYMENT`.

### 5. Pay for the booking

Replace `<BOOKING_ID>`. Use a fresh idempotency key for a new payment request; retry the same request with the same key if the outcome is unknown. Supported methods are `CARD`, `UPI`, and `WALLET`.

```bash
curl -X POST http://localhost:8080/api/bookings/<BOOKING_ID>/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 6b40bb0e-85be-4ad8-b16b-a6753a55ed63' \
  -d '{"method":"UPI"}'
```

The mock processor approves the charge and the booking becomes `CONFIRMED`. A declined payment would instead set `PAYMENT_FAILED`; the included mock processors approve every charge by default.

### 6. Cancel the booking (optional)

Replace `<BOOKING_ID>` with the booking ID. Cancellation returns the updated booking and a calculated refund amount.

```bash
curl -X POST http://localhost:8080/api/bookings/<BOOKING_ID>/cancellation
```

The configured policy calculates a full refund for a confirmed booking. Refund execution is not connected to a gateway in this sample.

## Booking lifecycle

| Current state | Operation | Result | Inventory / refund behavior |
| --- | --- | --- | --- |
| New request | Create booking | `PENDING_PAYMENT` | Holds inventory for the stay |
| `PENDING_PAYMENT` | Successful payment | `CONFIRMED` | Continues to hold inventory |
| `PENDING_PAYMENT` | Declined payment | `PAYMENT_FAILED` | Releases inventory |
| `PENDING_PAYMENT` | Cancel | `CANCELLED` | Releases inventory; no refund |
| `CONFIRMED` | Cancel | `CANCELLED` | Releases inventory; returns the policy's calculated refund |
| `PAYMENT_FAILED` or `CANCELLED` | Pay or cancel again | Conflict | No state change |

If a payment processor throws or returns no result, the response is `502 Bad Gateway` and the booking remains `PENDING_PAYMENT`, because the charge outcome is unknown. Retry with the same idempotency key. The application cannot atomically coordinate an external gateway charge with its in-memory state; a real adapter must use the same key with the gateway to safely resolve retries.

## API overview

All request and response bodies use JSON. Create operations return UUIDs used by later requests.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api` | API index with links to Swagger UI and the generated API document |
| `GET` | `/api/properties` | Search by `city`, `locality`, `checkIn`, `checkOut`, `guests`, `minPriceMinor`, `maxPriceMinor`, `minStarRating`, and repeated `amenities` query parameters |
| `POST` | `/api/portfolios` | Create an owner account (a portfolio may manage one or many properties) |
| `POST` | `/api/portfolios/{portfolioId}/properties` | Add a property to an owner portfolio |
| `GET` | `/api/portfolios` or `/api/portfolios/{portfolioId}` | Browse owner portfolios |
| `POST` | `/api/bookings` | Hold available room inventory and create a pending booking |
| `GET` | `/api/bookings/{bookingId}` | Read booking state |
| `POST` | `/api/bookings/{bookingId}/payments` | Pay using `CARD`, `UPI`, or `WALLET`; requires an `Idempotency-Key` header |
| `POST` | `/api/bookings/{bookingId}/cancellation` | Cancel a pending or confirmed booking and receive the calculated refund amount |

Successful requests currently return `200 OK`. Error responses use Spring `ProblemDetail` JSON.

| Status | When it is returned |
| --- | --- |
| `400 Bad Request` | Invalid or incomplete JSON, missing required values or headers, invalid dates, unsupported payment method, or other validation failure |
| `404 Not Found` | Portfolio, property, room type, or booking cannot be found |
| `409 Conflict` | Duplicate owner email, unavailable inventory, invalid booking state, or idempotency key reused for a different payment request |
| `502 Bad Gateway` | Payment processor throws or returns no outcome, so the charge result cannot be confirmed |

## Tests and coverage

Unit tests are organized under matching `controller`, `service`, `repo`, `utils`, `helper`, and `exception` test packages. The HTTP integration coverage is in `HotelBookingApiIntegrationTest`, with request bodies in `src/test/resources/requests`; cross-component workflow checks are described in [TEST_SCENARIOS.md](TEST_SCENARIOS.md).

Run all tests and enforce the configured 100% instruction, branch, and line coverage thresholds with:

```bash
./mvnw clean verify
```

On Windows, use `mvnw.cmd clean verify`. JaCoCo writes the HTML coverage report to `target/site/jacoco/index.html`. The build fails if any production class falls below the configured 100% coverage thresholds.

## Assumptions and design notes

- Persistence uses concurrent in-memory collections behind `PortfolioRepository` and `BookingRepository`. Data resets when the app restarts; the repository interfaces are the seam for adding a database implementation.
- An owner portfolio is the common representation for individual owners and chains. A standalone property is an owner portfolio with one property; it uses the same property-add operation as every other property.
- Owner email addresses are unique across portfolios, compared without case sensitivity. Duplicate registrations return a conflict.
- Property room inventory is a count of interchangeable rooms for each room type. A pending-payment booking holds inventory, a successful payment confirms it, and a failed payment or cancellation releases it.
- **Concurrency assumption: one application process.** Booking, payment, and cancellation use in-process locks. Inventory locks are keyed by property and room type, and payment locks by idempotency key. These locks coordinate threads only inside this single running process; they do not coordinate separate application instances. Running multiple instances requires shared transactional persistence and a distributed lock or equivalent database-level concurrency control. Owner-email uniqueness is likewise checked in-process here; a database-backed deployment must enforce it with a unique constraint.
- Check-in dates must be today or later; a stay must have check-out after check-in. Browsing without dates is allowed. Owner and guest email fields must be nonblank and match a basic `name@domain.suffix` format; this is a simple input check, not full email-address verification.
- Payment requests are idempotent within the running process. Reusing an `Idempotency-Key` with the same booking and method returns the original outcome; reusing it for a different request returns a conflict. Keys and records are in memory and are lost on restart. The key is also passed to the payment processor so a real gateway can apply its own idempotency protection.
- If a payment processor throws or returns no outcome, the API returns `502 Bad Gateway`, leaves the booking awaiting payment, and requires retries to use the same idempotency key. A real payment adapter must make that key idempotent with its gateway to resolve an unknown charge outcome safely. Gateway charging and in-memory repository writes are not one atomic transaction.
- `PricingStrategy` is the seam for seasonal, demand-based, or other dynamic pricing. Discovery and booking use the same strategy for nightly-rate filtering and the final total. `StandardPricingStrategy` uses the room's configured rate; a custom strategy can be marked `@Primary` to replace it.
- Swagger UI and the OpenAPI document are generated from the Spring MVC endpoints by springdoc.
- Prices and refunds are integer minor currency units (for example paise); the sample does not impose a currency conversion or tax model.
- Payment processors are mock adapters that approve every charge and return a mock transaction ID. Replace them behind `PaymentProcessor` to connect a gateway. The selected cancellation policy currently calculates a full refund; replace `CancellationPolicy` for property-specific or time-based rules. Refund execution itself is left to a payment gateway integration.
- Search criteria are evaluated by independent `PropertySearchFilter` beans. Availability is checked against the requested dates and counts peak concurrent room usage per night.
- Property-level search filters (location, amenities, star rating) and room-level filters (price and availability) are evaluated separately. Every room-level criterion must match the same room type for the property to appear.
- The API is a demonstration backend and does not include authentication, authorization, or owner/guest account management.
