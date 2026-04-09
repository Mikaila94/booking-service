# Booking Service Testing Guide
> Testing principles for our Kotlin + Spring Boot + PostgreSQL booking service

---

## The Core Mental Model

**Test business logic in isolation. Test integrations against real dependencies.**

```
Unit tests (60%)            → No Spring. Pure Kotlin. MockK only.
Integration tests (30%)     → Full stack with Testcontainers + PostgreSQL.
Slice tests (10%)           → One Spring layer. Use when needed.
```

**Why this distribution?**
- Our service layer is thin (mostly CRUD + validation)
- Real bugs hide in DB constraints, transactions, and concurrency
- PostgreSQL-specific features (UUIDs, TIMESTAMPTZ) require real DB testing
- Business logic complexity lives in entities and validators

---

## Principle 1: Default to No Spring

Before reaching for any Spring annotation, ask: **does this test actually need Spring?**

Business logic in a service class does NOT need Spring. Mock the repository, test the logic.

```kotlin
// PREFER THIS — no Spring context, runs instantly
class BookingServiceTest {

    private lateinit var bookingRepository: BookingRepository
    private lateinit var bookingService: BookingService

    @BeforeEach
    fun setup() {
        bookingRepository = mockk(relaxed = true)
        bookingService = BookingServiceImplementation(bookingRepository)
    }

    @Test
    fun `should activate booking before saving`() {
        // Given
        val request = CreateBookingRequest(
            userId = UUID.randomUUID(),
            startTime = Instant.now().plusSeconds(3600),
            endTime = Instant.now().plusSeconds(7200)
        )
        
        val capturedBooking = slot<Booking>()
        every { bookingRepository.save(capture(capturedBooking)) } answers { 
            firstArg<Booking>().apply { id = UUID.randomUUID() }
        }

        // When
        bookingService.createBooking(request)

        // Then
        assertEquals(BookingStatus.ACTIVE, capturedBooking.captured.status)
    }
}
```

**What to unit test:**
- ✅ Entity business logic (`Booking.isExpired()`, `Booking.activate()`)
- ✅ Custom validators (`BookingTimeValidator`)
- ✅ Mappers (`BookingMapper.toDTO()`)
- ✅ Service orchestration (verify lifecycle methods are called)
- ❌ Spring Data JPA methods (`save()`, `findById()`)
- ❌ Framework magic (`@Transactional`, `@Valid`)

---

## Principle 2: Use Slice Annotations Sparingly

**For this booking service, slice tests are mostly overkill.** Our service is simple CRUD + validation.

Slices make sense when you have:
- Complex controllers with heavy request transformation
- Custom repository queries with native SQL
- Security configurations with custom filters

**We don't have that yet.** Skip to integration tests instead.

| Layer | Annotation | When to use |
|---|---|---|
| Controller (MVC) | `@WebMvcTest(BookingController::class)` | Complex request/response mapping |
| Repository | `@DataJpaTest` | Custom native queries to verify |
| REST client | `@RestClientTest` | Testing HTTP client beans |
| JSON mapping | `@JsonTest` | Custom serializers/deserializers |

**Current recommendation:** Focus on unit tests (MockK) and integration tests (Testcontainers). Add slices later if needed.

```kotlin
// Example: Only if you have complex controller logic
@WebMvcTest(BookingController::class)
class BookingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var bookingService: BookingService

    @Test
    fun `should return 404 when booking not found`() {
        every { bookingService.findActiveBooking(any()) } returns null

        mockMvc.get("/api/booking/{id}", UUID.randomUUID())
            .andExpect { status { isNotFound() } }
    }
}
```

---

## Principle 3: `@SpringBootTest` for Integration Tests

Use `@SpringBootTest` when you need to verify that multiple layers work together with real PostgreSQL.

**For our booking service, this is where the real value is:**
- Database constraints (unique bookings, foreign keys)
- Transaction boundaries and rollback behavior
- Concurrent booking creation (race conditions)
- Full HTTP request → controller → service → repository → DB flow

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class BookingIntegrationTest {

    @Container
    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("booking_test")
    }

    @DynamicPropertySource
    @JvmStatic
    fun configureProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
    }

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @BeforeEach
    fun cleanup() {
        bookingRepository.deleteAll() // Explicit cleanup between tests
    }

    @Test
    fun `should create booking end-to-end`() {
        // Given
        val request = CreateBookingRequest(
            userId = UUID.randomUUID(),
            startTime = Instant.now().plusSeconds(3600),
            endTime = Instant.now().plusSeconds(7200)
        )

        // When
        val response = restTemplate.postForEntity(
            "/api/booking/create",
            request,
            BookingDTO::class.java
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body?.id)
        assertEquals(BookingStatus.ACTIVE.name, response.body?.status)
    }
}
```

---

## Principle 4: Use Constructor Injection — Always

Constructor injection makes mocking trivial and doesn't require Spring at all in unit tests.

```kotlin
// Production code — Kotlin's primary constructor handles this
@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val notificationService: NotificationService
) {
    fun createBooking(request: CreateBookingRequest): BookingDTO {
        // ... business logic
    }
}

// Test code — just instantiate with mocks, zero Spring needed
class BookingServiceTest {
    private val bookingRepository: BookingRepository = mockk()
    private val notificationService: NotificationService = mockk()
    private val bookingService = BookingService(bookingRepository, notificationService)
    
    @Test
    fun `should create booking`() {
        every { bookingRepository.save(any()) } returns mockBooking
        // ... test logic
    }
}
```

**Avoid field injection (`@Autowired` on properties) — it makes unit testing harder.**

---

## Principle 5: Know Your Mocking Tools

**We use MockK for Kotlin unit tests** because it's designed for Kotlin idioms (coroutines, extension functions, default parameters).

| Tool | Where | What it does |
|---|---|---|
| `mockk()` | Pure unit tests | Creates a mock, no Spring involved |
| `mockk(relaxed = true)` | Unit tests | Creates a mock that returns default values |
| `slot()` | Unit tests | Captures arguments passed to mocked methods |
| `@MockBean` | Spring slice/integration tests | Replaces a Spring-managed bean with a mock |
| `@SpyBean` | Spring slice/integration tests | Wraps a real bean so you can spy on it |

```kotlin
// Pure MockK - no Spring
class BookingServiceTest {
    private val repository = mockk<BookingRepository>()
    
    @Test
    fun `should capture saved booking`() {
        val slot = slot<Booking>()
        every { repository.save(capture(slot)) } returns mockBooking
        
        service.createBooking(request)
        
        assertEquals(BookingStatus.ACTIVE, slot.captured.status)
    }
}

// With Spring - use @MockBean
@WebMvcTest(BookingController::class)
class BookingControllerTest {
    @MockBean
    lateinit var bookingService: BookingService
    
    // ... test with Spring context
}
```

---

## Principle 6: Name Your Tests Like Specifications

**Use Kotlin's backtick syntax for natural language test names.**

```kotlin
// Kotlin/JUnit 5 style - reads like natural language
@Test
fun `should return null for expired bookings`() { }

@Test
fun `should throw exception when booking already cancelled`() { }

@Test
fun `should activate booking before saving to database`() { }
```

**Benefits:**
- No camelCase mental parsing needed
- Test output reads like specifications
- Better for stakeholders reading CI output
- JUnit 5 + Kotlin supports this natively

**Pattern:** `should <expected behavior> when <condition>` or `should <expected behavior> for <scenario>`

---

## Principle 7: Structure Every Test with AAA (Arrange-Act-Assert)

```kotlin
@Test
fun `should filter expired bookings from results`() {
    // Arrange (Given)
    val activeBooking = Booking().apply {
        endTime = Instant.now().plusSeconds(3600)
        status = BookingStatus.ACTIVE
    }
    val expiredBooking = Booking().apply {
        endTime = Instant.now().minusSeconds(3600)
        status = BookingStatus.ACTIVE
    }
    every { bookingRepository.findAll() } returns listOf(activeBooking, expiredBooking)

    // Act (When)
    val result = bookingService.getActiveBookings()

    // Assert (Then)
    assertEquals(1, result.size)
    assertEquals(activeBooking.id, result[0].id)
}
```

**Keep these sections visually separated** with comments or blank lines. It makes tests scannable.

---

## Principle 8: Avoid @DirtiesContext — Be Careful with @Transactional

`@DirtiesContext` forces Spring to reload the entire context, destroying the caching benefit that makes tests fast. If you need it, it's usually a sign that your test has a design problem (shared mutable state).

**For integration tests, prefer explicit cleanup:**

```kotlin
@SpringBootTest
@Testcontainers
class BookingIntegrationTest {

    @Autowired
    lateinit var bookingRepository: BookingRepository

    @BeforeEach
    fun cleanup() {
        bookingRepository.deleteAll() // Explicit - no hidden behavior
    }

    @Test
    fun `should create booking`() {
        // Test runs in a real transaction context
        // No rollback magic hiding issues
    }
}
```

**When `@Transactional` on tests is OK:**
- `@DataJpaTest` (it's the default and appropriate)
- When verifying transactional behavior (e.g., rollback on exception)

**When to avoid it:**
- Integration tests that verify end-to-end flows
- Tests that should verify committed data
- It can mask transaction-related bugs in your production code

---

## Principle 9: For Real DBs in Integration Tests, Use Testcontainers

**H2 is not PostgreSQL.** Our app uses PostgreSQL-specific features (UUID types, TIMESTAMPTZ, native queries). Use a real container.

```kotlin
@SpringBootTest
@Testcontainers
class BookingIntegrationTest {

    @Container
    companion object {
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("booking_test")
            .withReusableContainer(true) // Reuse across test classes
    }

    @DynamicPropertySource
    @JvmStatic
    fun configureProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
    }

    @Test
    fun `should enforce unique booking constraint`() {
        // Test against real PostgreSQL constraints
    }
}
```

**Why Testcontainers for our booking service:**
- Tests UUID column types correctly
- Tests TIMESTAMPTZ behavior with timezones
- Catches PostgreSQL-specific SQL syntax errors
- Verifies actual constraint enforcement

---

## Principle 10: Use a Base Class for Integration Tests (When You Have Many)

Spring caches the application context between tests — but only if the context configuration is identical. Extending a shared base class guarantees reuse and keeps your integration tests fast.

**For our booking service:** We only have a few integration test classes now. **Start without a base class.**

**When to introduce a base class:**
- You have 10+ integration test classes
- Test startup time becomes a problem
- You're duplicating Testcontainer setup everywhere

```kotlin
// Base class — context boots once for all subclasses
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("booking_test")
    }

    @DynamicPropertySource
    @JvmStatic
    fun configureProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
    }
}

// All integration test classes extend this
class BookingIntegrationTest : BaseIntegrationTest() { /* ... */ }
class UserIntegrationTest : BaseIntegrationTest() { /* ... */ }
```

**Alternative:** Use `withReusableContainer(true)` on your Testcontainer and let Gradle/JUnit handle context caching.

---

## Principle 11: Separate Test Config from Production Config

Put test-specific YAML in `src/test/resources/application-test.yml` and activate it with `@ActiveProfiles("test")`. This overrides production config without contaminating it.

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    show-sql: true
app:
  external-api:
    base-url: http://localhost:${wiremock.server.port}
```

---

## Principle 12: Mock External HTTP with WireMock

When your service calls another service over HTTP, don't let tests hit real endpoints. Use WireMock.

```kotlin
@SpringBootTest
@AutoConfigureWireMock(port = 0) // random port, injected into config
class PaymentServiceTest {

    @Test
    fun `should handle payment approval`() {
        stubFor(post("/api/charge")
            .willReturn(okJson("""{"status":"approved","transactionId":"abc123"}""")))

        val result = paymentService.charge(ChargeRequest(100.0))
        assertTrue(result.isApproved)
    }
}
```

---

## Principle 13: Relative Time/UUIDs Are Fine — Don't Over-Mock

**Your tests are already deterministic if they test relationships, not absolute values.**

```kotlin
// ✅ GOOD: Tests the relationship (endTime > startTime)
@Test
fun `should validate booking time range`() {
    val startTime = Instant.now().plusSeconds(3600)  // Any future time
    val endTime = startTime.plusSeconds(7200)        // Always after startTime
    
    val request = CreateBookingRequest(UUID.randomUUID(), startTime, endTime)
    
    assertTrue(validator.isValid(request))  // Always passes
}

// ❌ OVERKILL: Mocking the clock adds complexity for no benefit
@Test
fun `should validate booking time range`() {
    val fixedClock = Clock.fixed(Instant.parse("2026-03-09T10:00:00Z"), ZoneId.of("UTC"))
    val validator = BookingTimeValidator(fixedClock) // Now your prod code needs clock injection!
    
    val startTime = Instant.parse("2026-03-09T11:00:00Z")
    val endTime = Instant.parse("2026-03-09T13:00:00Z")
    
    assertTrue(validator.isValid(request))  // No real benefit
}
```

**When to use fixed values:**
- ✅ Testing exact value equality or serialization
- ✅ Testing scheduled/cron logic that depends on specific times
- ✅ Testing edge cases (leap years, DST transitions)

**When relative values are fine:**
- ✅ Testing relationships (`a > b`, `x != null`)
- ✅ Testing business logic that doesn't care about exact values
- ✅ Most unit tests in your service layer

**Don't add clock/UUID injection to your production code just for tests.**

---

## Principle 14: Test Behavior, Not Implementation

**Focus on the contract (what), not the mechanics (how).**

```kotlin
// ❌ BAD: Testing implementation details
@Test
fun `should call repository save`() {
    bookingService.createBooking(request)
    
    verify { bookingRepository.save(any()) }  // Who cares if save() was called?
}

// ✅ GOOD: Testing business behavior
@Test
fun `should activate booking before saving`() {
    val capturedBooking = slot<Booking>()
    every { bookingRepository.save(capture(capturedBooking)) } answers { firstArg() }

    bookingService.createBooking(request)

    assertEquals(BookingStatus.ACTIVE, capturedBooking.captured.status)  // This is the contract
}
```

**Red flags in your tests:**
- 🚩 Too many `verify {}` calls checking method invocations
- 🚩 Testing that mocks return what you told them to return
- 🚩 Tests that break when you refactor internal implementation
- 🚩 Test names like "should call X" instead of "should do Y"

**What to test:**
- ✅ State changes (status becomes ACTIVE)
- ✅ Return values (returns null for expired bookings)
- ✅ Thrown exceptions (throws when already cancelled)
- ✅ Business rules (endTime must be after startTime)

**What NOT to test:**
- ❌ Framework code (Spring Data JPA, @Transactional)
- ❌ Getters/setters (data classes)
- ❌ Private methods (test through public API)
- ❌ That your mocks work like you configured them

---

## Principle 15: Know What NOT to Test

**Not everything deserves a test. Focus on business logic and integration points.**

### ❌ Skip Testing:

**1. Framework Magic**
```kotlin
// Don't test that Spring Data JPA works
@Test
fun `should save booking`() {
    val booking = Booking(...)
    bookingRepository.save(booking)  // This is framework code
}
```

**2. Data Classes / DTOs**
```kotlin
// Don't test generated code
@Test
fun `should get booking id`() {
    val dto = BookingDTO(id = someId, ...)
    assertEquals(someId, dto.id)  // Pointless
}
```

**3. Simple Delegations**
```kotlin
// Don't test trivial pass-through
@Service
class BookingFacade(private val bookingService: BookingService) {
    fun getAll() = bookingService.getAll()  // Nothing to test here
}
```

### ✅ DO Test:

**1. Business Logic**
```kotlin
fun isExpired(): Boolean = Instant.now().isAfter(endTime)
fun activate() { /* validation + state change */ }
```

**2. Custom Validators**
```kotlin
class BookingTimeValidator : ConstraintValidator<ValidBookingTime, CreateBookingRequest>
```

**3. Complex Mappings**
```kotlin
fun toDTO(booking: Booking): BookingDTO {
    return BookingDTO(
        // ... transformation logic
        isExpired = booking.isExpired()
    )
}
```

**4. Integration Points**
- Database constraints and queries
- HTTP endpoints (full request/response)
- Transaction boundaries
- Concurrency behavior

---

## The Decision Checklist (Paste Above Your Test Class)

```
// Does this test need Spring?          No  → Pure Kotlin + MockK
// Testing a controller?                Yes → @WebMvcTest + @MockBean
// Testing a repository query?          Yes → @DataJpaTest
// Testing full flow / security?        Yes → @SpringBootTest (use for integration tests)
// Calling external HTTP?               Yes → WireMock
// Need a real DB in integration test?  Yes → Testcontainers
// State leaking between tests?         Fix → Explicit cleanup in @BeforeEach
```

---

## Quick Reference: Annotation Cheatsheet

```
// Pure Kotlin Unit Tests
mockk()                              Create a mock (MockK)
mockk(relaxed = true)                Create a mock with default return values
slot<T>()                            Capture arguments passed to mocks
every { mock.method() } returns x    Stub a method call
verify { mock.method() }             Verify a method was called

// Spring Test Annotations
@WebMvcTest(MyController::class)     Boot MVC layer only
@DataJpaTest                         Boot JPA + embedded DB only
@SpringBootTest                      Boot full app (use for integration tests)
@MockBean                            Replace Spring bean with mock
@SpyBean                             Wrap real Spring bean with spy
@ActiveProfiles("test")              Load application-test.yml
@Testcontainers + @Container         Spin up real Docker dependencies
@DynamicPropertySource               Wire container ports into Spring config
@Transactional (on test class)       Auto-rollback DB after each test (use carefully)
@DirtiesContext                      AVOID — reloads context, kills performance

// JUnit 5 + Kotlin
@Test                                Mark test method
@BeforeEach                          Run before each test
@AfterEach                           Run after each test
@Nested                              Group related tests
@DisplayName("...")                  Custom test display name
```

---

## Booking Service Test Strategy Summary

### Our Test Pyramid

```
   /\
  /  \    Integration Tests (30%)
 /____\   - Full stack with Testcontainers
/      \  - DB constraints & transactions
/________\ - HTTP → Controller → Service → DB
/__________\

  /\
 /  \       Slice Tests (10%)
/____\      - Only when complexity grows
/__________\ - @WebMvcTest, @DataJpaTest

    /\
   /  \
  /    \     Unit Tests (60%)
 /      \    - MockK, no Spring
/________\   - Entity business logic
/__________\ - Validators, mappers
```

### What to Focus On

**High Value Tests:**
1. **Entity business logic** (`Booking.isExpired()`, `Booking.activate()`, `Booking.cancel()`)
2. **Custom validators** (`BookingTimeValidator`)
3. **Integration tests** (DB constraints, concurrent bookings, transactions)
4. **Mappers** (Entity ↔ DTO transformation)

**Lower Value Tests:**
1. Spring Data JPA CRUD methods
2. Simple controller pass-throughs
3. Getters/setters
4. Framework behavior

### Test Distribution Goals

| Test Type | Target % | What it covers |
|-----------|----------|----------------|
| Unit (MockK) | 60% | Business rules in isolation |
| Integration (Testcontainers) | 30% | Real DB, full stack |
| Slice (@WebMvcTest, etc.) | 10% | One layer (when needed) |

**Why more integration tests than typical?**
- Our domain is simple (CRUD + validation)
- Real bugs hide in DB constraints and concurrency
- PostgreSQL-specific features require real testing
- Service layer is thin by design

### Red Flags to Watch For

🚩 **Test smells:**
- Tests breaking when refactoring internals
- Too many `verify {}` calls
- Mocking everything including the clock
- Testing that mocks return what you told them
- Tests with "should call X" in the name

✅ **Good test patterns:**
- Tests that verify business rules
- Tests that catch regressions
- Tests that document expected behavior
- Tests that remain stable during refactoring
- Fast feedback (<1 second for unit tests)

---

## Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests BookingServiceTest

# Specific test method (use full path with backticks)
./gradlew test --tests "BookingServiceTest.should activate booking before saving"

# With detailed output
./gradlew test --info

# Clean build + test
./gradlew clean test

# View HTML report
open build/reports/tests/test/index.html
```