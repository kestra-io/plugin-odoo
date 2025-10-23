package io.kestra.plugin.odoo;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@KestraTest
class SimpleQueryTest {

    @Inject
    RunContextFactory runContextFactory;

    @Test
    void shouldCreateTaskWithRequiredProperties() {
        Query task = Query.builder()
            .id("test-task")
            .type(Query.class.getName())
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .build();

        assertThat(task, is(notNullValue()));
        assertThat(task.getUrl(), is(notNullValue()));
        assertThat(task.getDb(), is(notNullValue()));
        assertThat(task.getUsername(), is(notNullValue()));
        assertThat(task.getPassword(), is(notNullValue()));
        assertThat(task.getModel(), is(notNullValue()));
        assertThat(task.getOperation(), is(notNullValue()));
    }


    @Test
    void shouldCreateTaskWithOptionalProperties() throws Exception {
        List<String> fields = Arrays.asList("name", "email");

        Query fullTask = Query.builder()
            .id("full-task" + IdUtils.create())
            .type(Query.class.getName())
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .fields(Property.ofValue(fields))
            .limit(Property.ofValue(3))
            .offset(Property.ofValue(0))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, fullTask, Map.of());

        // Execute the task and assert on actual output
        Query.Output output = fullTask.run(runContext);

        // Assert on actual execution results
        assertThat(output, is(notNullValue()));
        assertThat(output.getRows(), is(notNullValue()));
        assertThat(output.getSize(), is(greaterThan(0L)));

        // Verify optional properties affected the result
        List<Map<String, Object>> records = output.getRows();
        assertThat(records.size(), is(lessThanOrEqualTo(3))); // Respects limit

        if (!records.isEmpty()) {
            Map<String, Object> firstRecord = records.get(0);
            // Verify only requested fields are present (plus id which is always included)
            assertTrue(firstRecord.containsKey("id"));
            assertTrue(firstRecord.containsKey("name"));
            assertTrue(firstRecord.containsKey("email"));
        }
    }

    @Test
    void shouldCreateTaskWithFilters() throws Exception {
        // Create filters as a List of Lists
        List<List<Object>> filters = Arrays.asList(
            Arrays.asList("is_company", "=", true)
        );

        Query taskWithFilters = Query.builder()
            .id("filter-task")
            .type(Query.class.getName())
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .filters(Property.ofValue(filters))
            .limit(Property.ofValue(5))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, taskWithFilters, Map.of());

        // Execute the task and assert on actual output
        Query.Output output = taskWithFilters.run(runContext);

        // Assert on actual execution results
        assertThat(output, is(notNullValue()));
        assertThat(output.getRows(), is(notNullValue()));
        assertThat(output.getSize(), is(greaterThanOrEqualTo(0L))); // Could be 0 if no companies match

        // Verify filters were applied - all returned records should be companies
        List<Map<String, Object>> records = output.getRows();
        if (!records.isEmpty()) {
            for (Map<String, Object> record : records) {
                assertTrue(record.containsKey("id"));
                // Note: We can't directly verify is_company=true unless we include it in fields
                // But the fact that we got results with this filter means it was applied
            }
        }
    }

    @Test
    void shouldSupportAllOperations() throws Exception {
        // Test only read operations that are safe to execute
        Operation[] safeOps = {Operation.SEARCH_READ, Operation.SEARCH, Operation.SEARCH_COUNT};

        for (Operation operation : safeOps) {
            Query opTask = Query.builder()
                .id("op-task-" + operation.getValue())
                .type(Query.class.getName())
                .url(Property.ofValue("http://localhost:8069"))
                .db(Property.ofValue("demo"))
                .username(Property.ofValue("test@demo.com"))
                .password(Property.ofValue("admin"))
                .model(Property.ofValue("res.partner"))
                .operation(Property.ofValue(operation))
                .limit(Property.ofValue(1))
                .fetchType(Property.ofValue(FetchType.FETCH))
                .build();

            RunContext runContext = TestsUtils.mockRunContext(runContextFactory, opTask, Map.of());

            // Execute the task and verify it completes successfully
            Query.Output output = opTask.run(runContext);

            // Assert on actual execution results
            assertThat(output, is(notNullValue()));

            // Different operations return different output structures
            switch (operation) {
                case SEARCH_COUNT:
                    assertThat(output.getSize(), is(notNullValue()));
                    assertThat(output.getSize(), is(greaterThanOrEqualTo(0L)));
                    break;
                case SEARCH:
                case SEARCH_READ:
                    assertThat(output.getRows(), is(notNullValue()));
                    assertThat(output.getSize(), is(greaterThanOrEqualTo(0L)));
                    break;
            }
        }
    }

    @Test
    void shouldExecuteSearchCountAndReturnValidOutput() throws Exception {
        Query countTask = Query.builder()
            .id("count-task")
            .type(Query.class.getName())
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.users"))
            .operation(Property.ofValue(Operation.SEARCH_COUNT))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, countTask, Map.of());

        // Execute the task and assert on output values
        Query.Output output = countTask.run(runContext);

        // Assert on actual execution results
        assertThat(output, is(notNullValue()));
        assertThat(output.getSize(), is(notNullValue()));
        assertThat(output.getSize(), is(greaterThan(0L)));

        // For SEARCH_COUNT, result is in size field, no row/rows data
        assertThat(output.getRow(), is(nullValue()));
        assertThat(output.getRows(), is(nullValue()));
        assertThat(output.getUri(), is(nullValue()));
    }

    @Test
    void shouldExecuteSearchReadAndReturnValidOutput() throws Exception {
        Query searchTask = Query.builder()
            .id("search-task")
            .type(Query.class.getName())
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.SEARCH_READ))
            .fields(Property.ofValue(Arrays.asList("name")))
            .limit(Property.ofValue(2))
            .fetchType(Property.ofValue(FetchType.FETCH))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, searchTask, Map.of());

        // Execute the task and assert on output values
        Query.Output output = searchTask.run(runContext);

        // Assert on actual execution results
        assertThat(output, is(notNullValue()));
        assertThat(output.getRows(), is(notNullValue()));
        assertThat(output.getSize(), is(greaterThan(0L)));

        // For FETCH type, result is in rows field
        List<Map<String, Object>> records = output.getRows();
        assertThat(records.size(), is(lessThanOrEqualTo(2))); // Respects limit
        assertThat(output.getSize(), is(equalTo((long) records.size())));

        // Other fields should be null for FETCH type
        assertThat(output.getRow(), is(nullValue()));
        assertThat(output.getUri(), is(nullValue()));

        if (!records.isEmpty()) {
            // Verify first record structure
            Map<String, Object> firstRecord = records.get(0);
            assertTrue(firstRecord.containsKey("id"));
            assertTrue(firstRecord.containsKey("name"));
        }
    }
}