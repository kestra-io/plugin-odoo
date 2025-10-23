package io.kestra.plugin.odoo;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that execute actual Odoo queries against demo instance.
 * These tests require network access and are enabled only when ODOO_INTEGRATION_TESTS=true.
 */
@KestraTest
class QueryExecutionTest {

    @Inject
    private RunContextFactory runContextFactory;

    private RunContext runContext;

    @BeforeEach
    void setUp() {
        runContext = runContextFactory.of();
    }

    @Test
    void shouldExecuteSearchReadOperation() throws Exception {
        Query task = Query.builder()
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin")).model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .fields(Property.ofValue(Arrays.asList("name", "email")))
            .limit(Property.ofValue(5))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        Query.Output output = task.run(runContext);

        // Assert on actual output values
        assertThat(output, is(notNullValue()));
        assertThat(output.getRows(), is(notNullValue()));
        assertThat(output.getSize(), is(greaterThan(0L)));

        // Verify result is in rows field
        List<Map<String, Object>> records = output.getRows();
        assertThat(records.size(), is(lessThanOrEqualTo(5))); // Respects limit
        assertThat(output.getSize(), is(equalTo((long) records.size())));

        // Other fields should be null for FETCH type
        assertThat(output.getRow(), is(nullValue()));
        assertThat(output.getUri(), is(nullValue()));

        if (!records.isEmpty()) {
            // Verify first record has expected structure
            Map<String, Object> firstRecord = records.get(0);
            assertTrue(firstRecord.containsKey("id"));
            assertTrue(firstRecord.containsKey("name"));
        }
    }

    @Test
    void shouldExecuteSearchCountOperation() throws Exception {
        Query task = Query.builder()
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.users"))
            .operation(Property.ofValue(Operation.SEARCH_COUNT))
            .build();

        Query.Output output = task.run(runContext);

        // Assert on actual output values
        assertThat(output, is(notNullValue()));
        assertThat(output.getSize(), is(notNullValue()));
        assertThat(output.getSize(), is(greaterThan(0L)));

        // For SEARCH_COUNT, no row/rows/uri data
        assertThat(output.getRow(), is(nullValue()));
        assertThat(output.getRows(), is(nullValue()));
        assertThat(output.getUri(), is(nullValue()));
    }

    @Test
    void shouldExecuteSearchWithFilters() throws Exception {
        // Create filters for active users
        List<List<Object>> filters = Arrays.asList(
            Arrays.asList("active", "=", true)
        );

        Query task = Query.builder()
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.users"))
            .operation(Property.ofValue(Operation.SEARCH))
            .filters(Property.ofValue(filters))
            .limit(Property.ofValue(3))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        Query.Output output = task.run(runContext);

        // Assert on actual output values
        assertThat(output, is(notNullValue()));
        assertThat(output.getRows(), is(notNullValue()));
        assertThat(output.getSize(), is(greaterThan(0L)));

        // Verify result is in rows field (list of IDs for SEARCH operation)
        List<Map<String, Object>> ids = output.getRows();
        assertThat(ids.size(), is(lessThanOrEqualTo(3))); // Respects limit
        assertThat(output.getSize(), is(equalTo((long) ids.size())));

        // Other fields should be null for FETCH type
        assertThat(output.getRow(), is(nullValue()));
        assertThat(output.getUri(), is(nullValue()));
    }

    @Test
    void shouldHandleAuthenticationFailure() {
        Query task = Query.builder()
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("invalid_password"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .build();

        // Should throw exception for invalid credentials
        Exception exception = assertThrows(Exception.class, () -> {
            task.run(runContext);
        });

        assertThat(exception.getMessage(), containsString("authenticate"));
    }

    @Test
    void shouldHandleInvalidModel() {
        Query task = Query.builder()
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("invalid.model"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .build();

        // Should throw exception for invalid model
        Exception exception = assertThrows(Exception.class, () -> {
            task.run(runContext);
        });

        assertThat(exception.getMessage(), containsString("model"));
    }
}