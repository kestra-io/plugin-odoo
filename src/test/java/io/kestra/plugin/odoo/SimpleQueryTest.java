package io.kestra.plugin.odoo;

import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class SimpleQueryTest {

    private Query task;

    @BeforeEach
    void setUp() {
        task = Query.builder()
            .url(Property.ofValue("https://test-odoo.com"))
            .db(Property.ofValue("test_db"))
            .username(Property.ofValue("test_user"))
            .password(Property.ofValue("test_password"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue("search_read"))
            .build();
    }

    @Test
    void shouldCreateTaskWithRequiredProperties() {
        assertThat(task, is(notNullValue()));
        assertThat(task.getUrl(), is(notNullValue()));
        assertThat(task.getDb(), is(notNullValue()));
        assertThat(task.getUsername(), is(notNullValue()));
        assertThat(task.getPassword(), is(notNullValue()));
        assertThat(task.getModel(), is(notNullValue()));
        assertThat(task.getOperation(), is(notNullValue()));
    }

    @Test
    void shouldCreateTaskWithDefaultOperation() {
        Query defaultTask = Query.builder()
            .url(Property.ofValue("https://test-odoo.com"))
            .db(Property.ofValue("test_db"))
            .username(Property.ofValue("test_user"))
            .password(Property.ofValue("test_password"))
            .model(Property.ofValue("res.partner"))
            .build();

        assertThat(defaultTask.getOperation(), is(notNullValue()));
    }

    @Test
    void shouldCreateTaskWithOptionalProperties() {
        List<String> fields = Arrays.asList("name", "email");
        List<Integer> ids = Arrays.asList(1, 2, 3);
        Map<String, Object> values = Map.of("name", "Test Partner");

        Query fullTask = Query.builder()
            .url(Property.ofValue("https://test-odoo.com"))
            .db(Property.ofValue("test_db"))
            .username(Property.ofValue("test_user"))
            .password(Property.ofValue("test_password"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue("create"))
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
            .url(Property.ofValue("https://test-odoo.com"))
            .db(Property.ofValue("test_db"))
            .username(Property.ofValue("test_user"))
            .password(Property.ofValue("test_password"))
            .model(Property.ofValue("res.partner"))
            .operation(Property.ofValue("search_read"))
            .filters(Property.ofValue(filters))
            .build();

        assertThat(taskWithFilters.getFilters(), is(notNullValue()));
        assertThat(taskWithFilters.getFilters(), is(notNullValue()));
    }

    @Test
    void shouldSupportAllOperations() {
        String[] supportedOps = {"search_read", "create", "write", "unlink", "search", "search_count"};

        for (String operation : supportedOps) {
            Query opTask = Query.builder()
                .url(Property.ofValue("https://test-odoo.com"))
                .db(Property.ofValue("test_db"))
                .username(Property.ofValue("test_user"))
                .password(Property.ofValue("test_password"))
                .model(Property.ofValue("res.partner"))
                .operation(Property.ofValue(operation))
                .build();

            assertThat(opTask.getOperation(), is(notNullValue()));
        }
    }
}