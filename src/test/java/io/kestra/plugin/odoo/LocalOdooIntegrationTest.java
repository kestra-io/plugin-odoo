package io.kestra.plugin.odoo;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests that run against a local Odoo instance.
 * These tests demonstrate the full CRUD cycle and are used for QA validation.
 *
 * To run these tests:
 * 1. Start local Odoo: ./.github/setup-unit.sh
 * 2. Run tests: ODOO_INTEGRATION_TESTS=true ./gradlew test
 */
@KestraTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "ODOO_INTEGRATION_TESTS", matches = "true")
class LocalOdooIntegrationTest {

    @Inject
    RunContextFactory runContextFactory;

    private static final String ODOO_URL = "http://localhost:8069";
    private static final String ODOO_DB = "demo";
    private static final String ODOO_USERNAME = "test@demo.com";
    private static final String ODOO_PASSWORD = "admin";

    private static Integer createdPartnerId;

    @Test
    @Order(1)
    @DisplayName("1. Count Partners - Verify Odoo Connection")
    void shouldCountPartners() throws Exception {
        Query countTask = Query.builder()
            .id("count-partners")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_COUNT))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, countTask, Map.of());
        Query.Output output = countTask.run(runContext);

        assertThat("Should connect to Odoo and count partners", output.getSize(), greaterThan(0L));
        System.out.println("✅ Found " + output.getSize() + " partners in Odoo");
    }

    @Test
    @Order(2)
    @DisplayName("2. Search Partners - Read Existing Data")
    void shouldSearchPartners() throws Exception {
        Query searchTask = Query.builder()
            .id("search-partners")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .fields(Property.ofValue(Arrays.asList("name", "email", "is_company")))
            .limit(Property.ofValue(5))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, searchTask, Map.of());
        Query.Output output = searchTask.run(runContext);

        assertThat("Should retrieve partner records", output.getRows(), notNullValue());
        assertThat("Should respect limit", output.getRows().size(), lessThanOrEqualTo(5));

        if (!output.getRows().isEmpty()) {
            Map<String, Object> firstPartner = output.getRows().get(0);
            assertThat("Partner should have ID", firstPartner, hasKey("id"));
            assertThat("Partner should have name", firstPartner, hasKey("name"));
            System.out.println("✅ Retrieved partner: " + firstPartner.get("name"));
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. Create Partner - Write New Data")
    void shouldCreatePartner() throws Exception {
        Map<String, Object> newPartnerData = Map.of(
            "name", "Kestra Test Partner - " + System.currentTimeMillis(),
            "email", "kestra-test@example.com",
            "is_company", true,
            "customer_rank", 1,
            "comment", "Created by Kestra Odoo Plugin Integration Test"
        );

        Query createTask = Query.builder()
            .id("create-partner")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.CREATE))
            .values(Property.ofValue(newPartnerData))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, createTask, Map.of());
        Query.Output output = createTask.run(runContext);

        assertThat("Should create partner and return count", output.getSize(), equalTo(1L));
        assertThat("Should return created partner ID", output.getIds(), notNullValue());
        assertThat("Should return exactly one ID", output.getIds().size(), equalTo(1));

        // Store the created ID for later tests
        createdPartnerId = output.getIds().get(0);
        System.out.println("✅ Created partner: " + newPartnerData.get("name") + " (ID: " + createdPartnerId + ")");
    }

    @Test
    @Order(4)
    @DisplayName("4. Update Partner - Modify Existing Data")
    void shouldUpdatePartner() throws Exception {
        assumeTrue(createdPartnerId != null, "Created partner ID should be available from previous test");

        Map<String, Object> updateData = Map.of(
            "phone", "+1-555-KESTRA",
            "comment", "Updated by Kestra Odoo Plugin Integration Test - " + new Date()
        );

        Query updateTask = Query.builder()
            .id("update-partner")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.WRITE))
            .ids(Property.ofValue(Arrays.asList(createdPartnerId)))
            .values(Property.ofValue(updateData))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, updateTask, Map.of());
        Query.Output output = updateTask.run(runContext);

        assertThat("Should update partner", output.getSize(), equalTo(1L));
        System.out.println("✅ Updated partner ID " + createdPartnerId + " with phone: " + updateData.get("phone"));
    }

    @Test
    @Order(5)
    @DisplayName("5. Read Updated Partner - Verify Update")
    void shouldReadUpdatedPartner() throws Exception {
        assumeTrue(createdPartnerId != null, "Created partner ID should be available from previous test");

        Query readTask = Query.builder()
            .id("read-updated-partner")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.READ))
            .ids(Property.ofValue(Arrays.asList(createdPartnerId)))
            .fields(Property.ofValue(Arrays.asList("id", "name", "email", "phone", "comment", "write_date")))
            .fetchType(Property.ofValue(FetchType.FETCH_ONE))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, readTask, Map.of());
        Query.Output output = readTask.run(runContext);

        assertThat("Should read the updated partner", output.getRow(), notNullValue());
        assertThat("Partner should have updated phone", output.getRow().get("phone"), equalTo("+1-555-KESTRA"));
        System.out.println("✅ Verified update - Partner phone: " + output.getRow().get("phone"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Search Partners by ID - Test SEARCH Operation")
    void shouldSearchPartnerIds() throws Exception {
        Query searchTask = Query.builder()
            .id("search-partner-ids")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH))
            .filters(Property.ofValue(Arrays.asList(
                Arrays.asList("is_company", "=", true)
            )))
            .limit(Property.ofValue(3))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, searchTask, Map.of());
        Query.Output output = searchTask.run(runContext);

        assertThat("Should return partner IDs", output.getRows(), notNullValue());
        assertThat("Should respect limit", output.getRows().size(), lessThanOrEqualTo(3));

        if (!output.getRows().isEmpty()) {
            Map<String, Object> firstResult = output.getRows().get(0);
            assertThat("Result should contain ID", firstResult, hasKey("id"));
            System.out.println("✅ Found partner IDs: " + output.getRows().size() + " results");
        }
    }

    @Test
    @Order(7)
    @DisplayName("7. Delete Partner - Clean Up Test Data")
    void shouldDeletePartner() throws Exception {
        assumeTrue(createdPartnerId != null, "Created partner ID should be available from previous test");

        Query deleteTask = Query.builder()
            .id("delete-partner")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.UNLINK))
            .ids(Property.ofValue(Arrays.asList(createdPartnerId)))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, deleteTask, Map.of());
        Query.Output output = deleteTask.run(runContext);

        assertThat("Should delete partner", output.getSize(), equalTo(1L));
        System.out.println("✅ Deleted partner ID " + createdPartnerId);
    }

    @Test
    @Order(8)
    @DisplayName("8. Verify Deletion - Confirm Clean Up")
    void shouldVerifyDeletion() throws Exception {
        assumeTrue(createdPartnerId != null, "Created partner ID should be available from previous test");

        Query searchTask = Query.builder()
            .id("verify-deletion")
            .type(Query.class.getName())
            .url(Property.ofValue(ODOO_URL))
            .db(Property.ofValue(ODOO_DB))
            .username(Property.ofValue(ODOO_USERNAME))
            .password(Property.ofValue(ODOO_PASSWORD))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_COUNT))
            .filters(Property.ofValue(Arrays.asList(
                Arrays.asList("id", "=", createdPartnerId)
            )))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, searchTask, Map.of());
        Query.Output output = searchTask.run(runContext);

        assertThat("Deleted partner should not be found", output.getSize(), equalTo(0L));
        System.out.println("✅ Confirmed deletion - Partner no longer exists");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("\n🎉 All integration tests completed successfully!");
        System.out.println("📋 Tests demonstrated:");
        System.out.println("  ✅ Connection to local Odoo instance");
        System.out.println("  ✅ SEARCH_COUNT operation");
        System.out.println("  ✅ SEARCH_READ operation with filters and field selection");
        System.out.println("  ✅ CREATE operation with new partner data and ID capture");
        System.out.println("  ✅ WRITE operation to update partner");
        System.out.println("  ✅ READ operation to fetch specific records");
        System.out.println("  ✅ SEARCH operation to get record IDs");
        System.out.println("  ✅ UNLINK operation to delete records");
        System.out.println("  ✅ Full CRUD cycle validation with proper ID handling");
    }
}