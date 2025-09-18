package io.kestra.plugin.odoo;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
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
    void shouldCreateTaskWithOptionalProperties() {
        List<String> fields = Arrays.asList("name", "email");
        List<Integer> ids = Arrays.asList(1, 2, 3);
        Map<String, Object> values = Map.of("name", "Test Partner");

        Query fullTask = Query.builder()
            .id("full-task")
            .type(Query.class.getName())
            .url(Property.ofValue("http://localhost:8069"))
            .db(Property.ofValue("demo"))
            .username(Property.ofValue("test@demo.com"))
            .password(Property.ofValue("admin"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue(Operation.CREATE))
            .fields(Property.ofValue(fields))
            .ids(Property.ofValue(ids))
            .values(Property.ofValue(values))
            .limit(Property.ofValue(100))
            .offset(Property.ofValue(0))
            .build();

        assertThat(fullTask.getFields(), is(notNullValue()));
        assertThat(fullTask.getIds(), is(notNullValue()));
        assertThat(fullTask.getValues(), is(notNullValue()));
        assertThat(fullTask.getLimit(), is(notNullValue()));
        assertThat(fullTask.getOffset(), is(notNullValue()));
    }

    @Test
    void shouldCreateTaskWithFilters() {
        // Create filters as a List of Lists
        List<List<Object>> filters = Arrays.asList(
            Arrays.asList("is_company", "=", true),
            Arrays.asList("customer_rank", ">", 0)
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
            .build();

        assertThat(taskWithFilters.getFilters(), is(notNullValue()));
        assertThat(taskWithFilters.getFilters(), is(notNullValue()));
    }

    @Test
    void shouldSupportAllOperations() {
        Operation[] supportedOps = {Operation.SEARCH_READ, Operation.READ, Operation.CREATE, Operation.WRITE, Operation.UNLINK, Operation.SEARCH, Operation.SEARCH_COUNT};

        for (Operation operation : supportedOps) {
            Query opTask = Query.builder()
                .id("op-task-" + operation.getValue())
                .type(Query.class.getName())
                .url(Property.ofValue("http://localhost:8069"))
                .db(Property.ofValue("demo"))
                .username(Property.ofValue("test@demo.com"))
                .password(Property.ofValue("admin"))
                .model(Property.ofValue("res.partner"))
                .operation(Property.ofValue(operation))
                .build();

            assertThat(opTask.getOperation(), is(notNullValue()));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ODOO_INTEGRATION_TESTS", matches = "true")
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
    @EnabledIfEnvironmentVariable(named = "ODOO_INTEGRATION_TESTS", matches = "true")
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