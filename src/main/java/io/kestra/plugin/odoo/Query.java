package io.kestra.plugin.odoo;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Query Odoo ERP system via XML-RPC API",
    description = "This task allows you to interact with Odoo models using XML-RPC API. " +
                  "It supports various operations like search_read, create, write, unlink, search, and search_count. " +
                  "You can configure the Odoo server connection and specify the model, operation, and parameters."
)
@Plugin(
    examples = {
        @Example(
            title = "Search and read company partners",
            full = true,
            code = """
                id: odoo_query_partners
                namespace: company.team

                tasks:
                  - id: query_partners
                    type: io.kestra.plugin.odoo.Query
                    url: https://my-odoo-instance.com
                    db: my-database
                    username: user@example.com
                    password: supersecret
                    model: res.partner
                    operation: search_read
                    filters:
                      - ["is_company", "=", true]
                      - ["customer_rank", ">", 0]
                    fields: ["name", "email", "phone", "is_company"]
                    limit: 10
                """
        ),
        @Example(
            title = "Create a new partner",
            code = """
                id: create_partner
                type: io.kestra.plugin.odoo.Query
                url: https://my-odoo-instance.com
                db: my-database
                username: user@example.com
                password: supersecret
                model: res.partner
                operation: create
                values:
                  name: "New Partner"
                  email: "partner@example.com"
                  is_company: true
                """
        ),
        @Example(
            title = "Update existing partners",
            code = """
                id: update_partners
                type: io.kestra.plugin.odoo.Query
                url: https://my-odoo-instance.com
                db: my-database
                username: user@example.com
                password: supersecret
                model: res.partner
                operation: write
                ids: [1, 2, 3]
                values:
                  category_id: [[6, 0, [1, 2]]]
                """
        ),
        @Example(
            title = "Count records with filters",
            code = """
                id: count_active_users
                type: io.kestra.plugin.odoo.Query
                url: https://my-odoo-instance.com
                db: my-database
                username: user@example.com
                password: supersecret
                model: res.users
                operation: search_count
                filters:
                  - ["active", "=", true]
                """
        )
    }
)
public class Query extends Task implements RunnableTask<Query.Output> {

    @Schema(
        title = "Odoo server URL",
        description = "The base URL of your Odoo instance (e.g., https://my-odoo-instance.com)"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private Property<String> url;

    @Schema(
        title = "Database name",
        description = "The name of the Odoo database to connect to"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private Property<String> db;

    @Schema(
        title = "Username",
        description = "Odoo username for authentication"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private Property<String> username;

    @Schema(
        title = "Password",
        description = "Odoo password for authentication"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private Property<String> password;

    @Schema(
        title = "Model name",
        description = "The Odoo model to operate on (e.g., 'res.partner', 'sale.order', 'product.product')"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    private Property<String> model;

    @Schema(
        title = "Operation",
        description = "The operation to perform on the model",
        allowableValues = {"search_read", "create", "write", "unlink", "search", "search_count"}
    )
    @PluginProperty(dynamic = true)
    @Builder.Default
    private Property<String> operation = Property.ofValue("search_read");

    @Schema(
        title = "Search filters",
        description = "Domain filters for search operations. Each filter is a list of [field, operator, value]. " +
                      "Multiple filters are combined with AND logic. Example: [[\"is_company\", \"=\", true], [\"customer_rank\", \">\", 0]]"
    )
    @PluginProperty(dynamic = true)
    private Property<List> filters;

    @Schema(
        title = "Fields to retrieve",
        description = "List of field names to retrieve in search_read operations. If not specified, all fields are returned."
    )
    @PluginProperty(dynamic = true)
    private Property<List<String>> fields;

    @Schema(
        title = "Values",
        description = "Field values for create/write operations. Map of field names to values."
    )
    @PluginProperty(dynamic = true)
    private Property<Map<String, Object>> values;

    @Schema(
        title = "Record IDs",
        description = "List of record IDs for write/unlink operations"
    )
    @PluginProperty(dynamic = true)
    private Property<List<Integer>> ids;

    @Schema(
        title = "Limit",
        description = "Maximum number of records to return (for search operations)"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> limit;

    @Schema(
        title = "Offset",
        description = "Number of records to skip (for search operations)"
    )
    @PluginProperty(dynamic = true)
    private Property<Integer> offset;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render dynamic properties
        String renderedUrl = runContext.render(this.url).as(String.class).orElseThrow();
        String renderedDb = runContext.render(this.db).as(String.class).orElseThrow();
        String renderedUsername = runContext.render(this.username).as(String.class).orElseThrow();
        String renderedPassword = runContext.render(this.password).as(String.class).orElseThrow();
        String renderedModel = runContext.render(this.model).as(String.class).orElseThrow();
        String renderedOperation = runContext.render(this.operation).as(String.class).orElse("search_read");

        logger.info("Connecting to Odoo server: {} with database: {}", renderedUrl, renderedDb);

        // Initialize Odoo client and authenticate
        OdooClient odooClient = new OdooClient(renderedUrl, renderedDb, renderedUsername, renderedPassword);
        odooClient.authenticate();

        logger.info("Authentication successful. Executing {} operation on model {}", renderedOperation, renderedModel);

        Object result;
        int recordCount = 0;

        switch (renderedOperation.toLowerCase()) {
            case "search_read":
                result = executeSearchRead(runContext, odooClient, renderedModel);
                recordCount = result instanceof List ? ((List<?>) result).size() : 0;
                break;

            case "create":
                result = executeCreate(runContext, odooClient, renderedModel);
                recordCount = 1;
                break;

            case "write":
                result = executeWrite(runContext, odooClient, renderedModel);
                recordCount = runContext.render(this.ids).asList(Integer.class).size();
                break;

            case "unlink":
                result = executeUnlink(runContext, odooClient, renderedModel);
                recordCount = runContext.render(this.ids).asList(Integer.class).size();
                break;

            case "search":
                result = executeSearch(runContext, odooClient, renderedModel);
                recordCount = result instanceof List ? ((List<?>) result).size() : 0;
                break;

            case "search_count":
                result = executeSearchCount(runContext, odooClient, renderedModel);
                recordCount = result instanceof Integer ? (Integer) result : 0;
                break;

            default:
                throw new IllegalArgumentException("Unsupported operation: " + renderedOperation +
                    ". Supported operations are: search_read, create, write, unlink, search, search_count");
        }

        logger.info("Operation {} completed successfully. Records affected/returned: {}", renderedOperation, recordCount);

        return Output.builder()
            .result(result)
            .operation(renderedOperation)
            .model(renderedModel)
            .recordCount(recordCount)
            .build();
    }

    @SuppressWarnings("unchecked")
    private Object executeSearchRead(RunContext runContext, OdooClient client, String model) throws Exception {
        List domain = null;
        if (this.filters != null) {
            domain = runContext.render(this.filters).as(List.class).orElse(null);
        }

        List<String> fieldsList = runContext.render(this.fields).asList(String.class);
        Integer limitValue = runContext.render(this.limit).as(Integer.class).orElse(null);
        Integer offsetValue = runContext.render(this.offset).as(Integer.class).orElse(null);

        return client.searchRead(model, domain, fieldsList, limitValue, offsetValue);
    }

    private Object executeCreate(RunContext runContext, OdooClient client, String model) throws Exception {
        Map<String, Object> valuesMap = runContext.render(this.values).asMap(String.class, Object.class);
        if (valuesMap == null || valuesMap.isEmpty()) {
            throw new IllegalArgumentException("Values are required for create operation");
        }
        return client.create(model, valuesMap);
    }

    private Object executeWrite(RunContext runContext, OdooClient client, String model) throws Exception {
        List<Integer> idsList = runContext.render(this.ids).asList(Integer.class);
        Map<String, Object> valuesMap = runContext.render(this.values).asMap(String.class, Object.class);

        if (idsList == null || idsList.isEmpty()) {
            throw new IllegalArgumentException("IDs are required for write operation");
        }
        if (valuesMap == null || valuesMap.isEmpty()) {
            throw new IllegalArgumentException("Values are required for write operation");
        }

        return client.write(model, idsList, valuesMap);
    }

    private Object executeUnlink(RunContext runContext, OdooClient client, String model) throws Exception {
        List<Integer> idsList = runContext.render(this.ids).asList(Integer.class);
        if (idsList == null || idsList.isEmpty()) {
            throw new IllegalArgumentException("IDs are required for unlink operation");
        }
        return client.unlink(model, idsList);
    }

    @SuppressWarnings("unchecked")
    private Object executeSearch(RunContext runContext, OdooClient client, String model) throws Exception {
        List domain = null;
        if (this.filters != null) {
            domain = runContext.render(this.filters).as(List.class).orElse(null);
        }

        Integer limitValue = runContext.render(this.limit).as(Integer.class).orElse(null);
        Integer offsetValue = runContext.render(this.offset).as(Integer.class).orElse(null);

        return client.search(model, domain, limitValue, offsetValue);
    }

    @SuppressWarnings("unchecked")
    private Object executeSearchCount(RunContext runContext, OdooClient client, String model) throws Exception {
        List domain = null;
        if (this.filters != null) {
            domain = runContext.render(this.filters).as(List.class).orElse(null);
        }

        return client.searchCount(model, domain);
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Operation result",
            description = "The result data returned by the Odoo operation. Format varies by operation type."
        )
        private final Object result;

        @Schema(
            title = "Operation performed",
            description = "The operation that was executed"
        )
        private final String operation;

        @Schema(
            title = "Model name",
            description = "The Odoo model that was operated on"
        )
        private final String model;

        @Schema(
            title = "Record count",
            description = "Number of records affected or returned by the operation"
        )
        private final Integer recordCount;
    }
}