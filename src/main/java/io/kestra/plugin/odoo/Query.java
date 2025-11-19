package io.kestra.plugin.odoo;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.models.tasks.common.FetchType;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Query Odoo ERP system via XML-RPC API.",
    description = "This task interacts with Odoo models using XML-RPC API. " +
                  "It supports various operations like search_read, create, write, unlink, search, and search_count. " +
                  "You can configure the Odoo server connection and specify the model, operation, and parameters."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Search and read company partners.",
            code = """
                id: odoo_query_partners
                namespace: company.team

                tasks:
                  - id: query_partners
                    type: io.kestra.plugin.odoo.Query
                    url: http://localhost:8069
                    db: demo
                    username: test@demo.com
                    password: "{{ secret('ODOO_PASSWORD') }}"
                    model: res.partner
                    operation: SEARCH_READ
                    filters:
                      - ["is_company", "=", true]
                    fields: ["name", "email", "phone", "is_company"]
                    limit: 10
                    fetchType: FETCH
                """
        ),
        @Example(
            full = true,
            title = "Create a new partner.",
            code = """
                id: create_partner
                namespace: company.team

                tasks:
                  - id: create_partner
                    type: io.kestra.plugin.odoo.Query
                    url: http://localhost:8069
                    db: demo
                    username: test@demo.com
                    password: "{{ secret('ODOO_PASSWORD') }}"
                    model: res.partner
                    operation: CREATE
                    values:
                      name: "Acme Corporation"
                      email: "contact@acme.com"
                      is_company: true
                """
        ),
        @Example(
            full = true,
            title = "Update existing partners.",
            code = """
                id: update_partners
                namespace: company.team

                tasks:
                  - id: update_partners
                    type: io.kestra.plugin.odoo.Query
                    url: http://localhost:8069
                    db: demo
                    username: test@demo.com
                    password: "{{ secret('ODOO_PASSWORD') }}"
                    model: res.partner
                    operation: WRITE
                    ids: [1, 2, 3]
                    values:
                      active: true
                      category_id: [[6, 0, [1, 2]]]
                """
        ),
        @Example(
            full = true,
            title = "Read specific partner records by ID.",
            code = """
                id: read_partners
                namespace: company.team

                tasks:
                  - id: read_partners
                    type: io.kestra.plugin.odoo.Query
                    url: http://localhost:8069
                    db: demo
                    username: test@demo.com
                    password: "{{ secret('ODOO_PASSWORD') }}"
                    model: res.partner
                    operation: READ
                    ids: [1, 2, 3]
                    fields: ["name", "email", "phone"]
                    fetchType: FETCH
                """
        ),
    }
)
public class Query extends Task implements RunnableTask<Query.Output> {

    @Schema(
        title = "Odoo server URL",
        description = "The base URL of your Odoo instance (e.g., https://my-odoo-instance.com)"
    )
    @NotNull
    private Property<String> url;

    @Schema(
        title = "Database name",
        description = "The name of the Odoo database to connect to"
    )
    @NotNull
    private Property<String> db;

    @Schema(
        title = "Username",
        description = "Odoo username for authentication"
    )
    @NotNull
    private Property<String> username;

    @Schema(
        title = "Password",
        description = "Odoo password for authentication"
    )
    @NotNull
    private Property<String> password;

    @Schema(
        title = "Model name",
        description = "The Odoo model to operate on (e.g., 'res.partner', 'sale.order', 'product.product')"
    )
    @NotNull
    private Property<String> model;

    @Schema(
        title = "Operation",
        description = "The operation to perform on the model"
    )
    @NotNull
    private Property<Operation> operation;

    @Schema(
        title = "Search filters",
        description = "Domain filters for search operations. Each filter is a list of [field, operator, value]. " +
                      "Multiple filters are combined with AND logic. Example: [[\"is_company\", \"=\", true], [\"customer_rank\", \">\", 0]]"
    )
    private Property<List<?>> filters;

    @Schema(
        title = "Fields to retrieve",
        description = "List of field names to retrieve in search_read operations. If not specified, all fields are returned."
    )
    private Property<List<String>> fields;

    @Schema(
        title = "Values",
        description = "Field values for create/write operations. Map of field names to values."
    )
    private Property<Map<String, Object>> values;

    @Schema(
        title = "Record IDs",
        description = "List of record IDs for write/unlink operations"
    )
    private Property<List<Integer>> ids;

    @Schema(
        title = "Limit",
        description = "Maximum number of records to return (for search operations)"
    )
    private Property<Integer> limit;

    @Schema(
        title = "Offset",
        description = "Number of records to skip (for search operations)"
    )
    private Property<Integer> offset;

    @Schema(
        title = "Fetch type",
        description = "How to handle query results. STORE: store all rows to a file, FETCH: output all rows as output variable, FETCH_ONE: output the first row, NONE: do nothing (default for non-read operations)"
    )
    @NotNull
    @Builder.Default
    private Property<FetchType> fetchType = Property.ofValue(FetchType.NONE);

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Render dynamic properties
        String rUrl = runContext.render(this.url).as(String.class).orElseThrow();
        String rDb = runContext.render(this.db).as(String.class).orElseThrow();
        String rUsername = runContext.render(this.username).as(String.class).orElseThrow();
        String rPassword = runContext.render(this.password).as(String.class).orElseThrow();
        String rModel = runContext.render(this.model).as(String.class).orElseThrow();
        Operation rOperation = runContext.render(this.operation).as(Operation.class).orElseThrow();
        FetchType rFetchType = runContext.render(this.fetchType).as(FetchType.class).orElse(FetchType.NONE);

        logger.info("Connecting to Odoo server: {} with database: {}", rUrl, rDb);

        // Initialize Odoo client and authenticate
        OdooClient odooClient = new OdooClient(rUrl, rDb, rUsername, rPassword, logger);
        odooClient.authenticate();

        logger.info("Authentication successful. Executing {} operation on model {}", rOperation.getValue(), rModel);

        Object result;
        int recordCount;

        switch (rOperation) {
            case SEARCH_READ: {
                Object searchReadResult = executeSearchRead(runContext, odooClient, rModel, rFetchType);
                result = searchReadResult;
                recordCount = calculateRecordCount(searchReadResult, rFetchType);
                break;
            }

            case READ: {
                Object readResult = executeRead(runContext, odooClient, rModel, rFetchType);
                result = readResult;
                recordCount = calculateRecordCount(readResult, rFetchType);
                break;
            }

            case CREATE: {
                result = executeCreate(runContext, odooClient, rModel);
                recordCount = 1;
                break;
            }

            case WRITE: {
                result = executeWrite(runContext, odooClient, rModel);
                recordCount = runContext.render(this.ids).asList(Integer.class).size();
                break;
            }

            case UNLINK: {
                result = executeUnlink(runContext, odooClient, rModel);
                recordCount = runContext.render(this.ids).asList(Integer.class).size();
                break;
            }

            case SEARCH: {
                Object searchResult = executeSearch(runContext, odooClient, rModel, rFetchType);
                result = searchResult;
                recordCount = calculateRecordCount(searchResult, rFetchType);
                break;
            }

            case SEARCH_COUNT: {
                Integer count = executeSearchCount(runContext, odooClient, rModel);
                result = count;
                recordCount = count != null ? count : 0;
                break;
            }

            default:
                throw new IllegalArgumentException("Unsupported operation: " + rOperation.getValue() +
                    ". Supported operations are: search_read, read, create, write, unlink, search, search_count");
        }

        logger.info("Operation {} completed successfully. Records affected/returned: {}", rOperation.getValue(), recordCount);

        // Build output based on fetch type and operation
        Output.OutputBuilder outputBuilder = Output.builder().size((long) recordCount);

        if (rOperation == Operation.SEARCH_READ || rOperation == Operation.READ) {
            switch (rFetchType) {
                case FETCH_ONE:
                    @SuppressWarnings("unchecked")
                    Map<String, Object> row = result instanceof Map ? (Map<String, Object>) result : null;
                    outputBuilder.row(row);
                    break;
                case FETCH:
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rows = result instanceof List ? (List<Map<String, Object>>) result : null;
                    outputBuilder.rows(rows);
                    break;
                case STORE:
                    outputBuilder.uri(result instanceof StoredResult ? ((StoredResult) result).getUri() : null);
                    break;
                case NONE:
                default:
                    // No output data for NONE
                    break;
            }
        } else if (rOperation == Operation.SEARCH) {
            // SEARCH returns list of IDs, need to convert to proper format
            switch (rFetchType) {
                case FETCH_ONE:
                    if (result instanceof Integer) {
                        outputBuilder.row(Map.of("id", result));
                    } else if (result instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<?> resultList = (List<?>) result;
                        if (!resultList.isEmpty()) {
                            outputBuilder.row(Map.of("id", resultList.get(0)));
                        }
                    }
                    break;
                case FETCH:
                    if (result instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<?> resultList = (List<?>) result;
                        List<Map<String, Object>> convertedIds = resultList.stream()
                            .map(id -> Map.<String, Object>of("id", id))
                            .toList();
                        outputBuilder.rows(convertedIds);
                    }
                    break;
                case STORE:
                    outputBuilder.uri(result instanceof StoredResult ? ((StoredResult) result).getUri() : null);
                    break;
                case NONE:
                default:
                    // No output data for NONE
                    break;
            }
        } else if (rOperation == Operation.CREATE) {
            // CREATE returns the ID of the created record
            if (result instanceof Integer) {
                outputBuilder.ids(List.of((Integer) result));
            }
        } else if (rOperation == Operation.WRITE || rOperation == Operation.UNLINK) {
            // WRITE and UNLINK operations - populate ids with the affected record IDs
            List<Integer> affectedIds = runContext.render(this.ids).asList(Integer.class);
            if (affectedIds != null && !affectedIds.isEmpty()) {
                outputBuilder.ids(affectedIds);
            }
        }

        return outputBuilder.build();
    }

    /**
     * Execute SEARCH_READ operation - returns List<Map<String, Object>> for records data.
     * Return type varies based on fetchType: List<Map> for FETCH, Map for FETCH_ONE, StoredResult for STORE, null for NONE.
     */
    @SuppressWarnings("unchecked")
    private Object executeSearchRead(RunContext runContext, OdooClient client, String model, FetchType fetchType) throws Exception {
        List<?> domain = null;
        if (this.filters != null) {
            domain = runContext.render(this.filters).asList(Object.class);
        }

        List<String> fieldsList = runContext.render(this.fields).asList(String.class);
        Integer limitValue = runContext.render(this.limit).as(Integer.class).orElse(null);
        Integer offsetValue = runContext.render(this.offset).as(Integer.class).orElse(null);

        List<?> records = (List<?>) client.searchRead(model, domain, fieldsList, limitValue, offsetValue);

        // Handle fetchType logic
        switch (fetchType) {
            case FETCH_ONE:
                return records.isEmpty() ? null : records.get(0);
            case NONE:
                return null;
            case STORE:
                return storeRecordsAsFile(runContext, records);
            case FETCH:
            default:
                return records;
        }
    }

    /**
     * Execute READ operation - returns List<Map<String, Object>> for records data.
     * Return type varies based on fetchType: List<Map> for FETCH, Map for FETCH_ONE, StoredResult for STORE, null for NONE.
     */
    private Object executeRead(RunContext runContext, OdooClient client, String model, FetchType fetchType) throws Exception {
        List<Integer> idsList = runContext.render(this.ids).asList(Integer.class);
        if (idsList == null || idsList.isEmpty()) {
            throw new IllegalArgumentException("IDs are required for read operation");
        }

        List<String> fieldsList = runContext.render(this.fields).asList(String.class);

        List<?> records = (List<?>) client.read(model, idsList, fieldsList);

        // Handle fetchType logic
        switch (fetchType) {
            case FETCH_ONE:
                return records.isEmpty() ? null : records.get(0);
            case NONE:
                return null;
            case STORE:
                return storeRecordsAsFile(runContext, records);
            case FETCH:
            default:
                return records;
        }
    }

    /**
     * Execute CREATE operation - returns Integer (ID of the created record).
     */
    private Integer executeCreate(RunContext runContext, OdooClient client, String model) throws Exception {
        Map<String, Object> valuesMap = runContext.render(this.values).asMap(String.class, Object.class);
        if (valuesMap == null || valuesMap.isEmpty()) {
            throw new IllegalArgumentException("Values are required for create operation");
        }
        Object result = client.create(model, valuesMap);
        return result instanceof Integer ? (Integer) result : null;
    }

    /**
     * Execute WRITE operation - returns Boolean (success/failure of the update).
     */
    private Boolean executeWrite(RunContext runContext, OdooClient client, String model) throws Exception {
        List<Integer> idsList = runContext.render(this.ids).asList(Integer.class);
        Map<String, Object> valuesMap = runContext.render(this.values).asMap(String.class, Object.class);

        if (idsList == null || idsList.isEmpty()) {
            throw new IllegalArgumentException("IDs are required for write operation");
        }
        if (valuesMap == null || valuesMap.isEmpty()) {
            throw new IllegalArgumentException("Values are required for write operation");
        }

        Object result = client.write(model, idsList, valuesMap);
        return result instanceof Boolean ? (Boolean) result : Boolean.TRUE;
    }

    /**
     * Execute UNLINK operation - returns Boolean (success/failure of the deletion).
     */
    private Boolean executeUnlink(RunContext runContext, OdooClient client, String model) throws Exception {
        List<Integer> idsList = runContext.render(this.ids).asList(Integer.class);
        if (idsList == null || idsList.isEmpty()) {
            throw new IllegalArgumentException("IDs are required for unlink operation");
        }
        Object result = client.unlink(model, idsList);
        return result instanceof Boolean ? (Boolean) result : Boolean.TRUE;
    }

    /**
     * Execute SEARCH operation - returns List<Integer> for record IDs.
     * Return type varies based on fetchType: List<Integer> for FETCH, Integer for FETCH_ONE, StoredResult for STORE, null for NONE.
     */
    @SuppressWarnings("unchecked")
    private Object executeSearch(RunContext runContext, OdooClient client, String model, FetchType fetchType) throws Exception {
        List<?> domain = null;
        if (this.filters != null) {
            domain = runContext.render(this.filters).asList(Object.class);
        }

        Integer limitValue = runContext.render(this.limit).as(Integer.class).orElse(null);
        Integer offsetValue = runContext.render(this.offset).as(Integer.class).orElse(null);

        List<?> ids = (List<?>) client.search(model, domain, limitValue, offsetValue);

        // Handle fetchType logic
        switch (fetchType) {
            case FETCH_ONE:
                return ids.isEmpty() ? null : ids.get(0);
            case NONE:
                return null;
            case STORE:
                // For SEARCH operation, we need to convert IDs to a proper format for storage
                List<Map<String, Object>> convertedIds = ids.stream()
                    .map(id -> Map.<String, Object>of("id", id))
                    .toList();
                return storeRecordsAsFile(runContext, convertedIds);
            case FETCH:
            default:
                return ids;
        }
    }

    /**
     * Execute SEARCH_COUNT operation - returns Integer (count of records matching criteria).
     */
    @SuppressWarnings("unchecked")
    private Integer executeSearchCount(RunContext runContext, OdooClient client, String model) throws Exception {
        List<?> domain = null;
        if (this.filters != null) {
            domain = runContext.render(this.filters).asList(Object.class);
        }

        Object result = client.searchCount(model, domain);
        return result instanceof Integer ? (Integer) result : 0;
    }

    /**
     * Store records as an ION file in Kestra's internal storage.
     * Returns a StoredResult containing both the URI and record count.
     * TODO: Implement proper file storage once storage interface is clarified
     */
    private StoredResult storeRecordsAsFile(RunContext runContext, List<?> records) throws IOException {
        // Temporary implementation - return null URI but correct count
        // This needs to be implemented with proper storage interface
        return new StoredResult(null, records.size());
    }

    /**
     * Helper class to return both URI and count for STORE operations.
     */
    private static class StoredResult {
        private final URI uri;
        private final int count;

        StoredResult(URI uri, int count) {
            this.uri = uri;
            this.count = count;
        }

        public URI getUri() { return uri; }
        public int getCount() { return count; }
    }

    /**
     * Calculate record count based on result and fetch type.
     */
    private int calculateRecordCount(Object result, FetchType fetchType) {
        if (result == null) {
            return 0;
        }

        switch (fetchType) {
            case FETCH_ONE:
                return result != null ? 1 : 0;
            case NONE:
                return 0;
            case STORE:
                return result instanceof StoredResult ? ((StoredResult) result).getCount() : 0;
            case FETCH:
            default:
                return result instanceof List ? ((List<?>) result).size() : 0;
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Map containing the first row of fetched data",
            description = "Only populated if fetchType is FETCH_ONE."
        )
        private final Map<String, Object> row;

        @Schema(
            title = "List of map containing rows of fetched data",
            description = "Only populated if fetchType is FETCH."
        )
        private final List<Map<String, Object>> rows;

        @Schema(
            title = "The URI of the result file on Kestra's internal storage (.ion file / Amazon Ion formatted text file)",
            description = "Only populated if fetchType is STORE."
        )
        private final URI uri;

        @Schema(
            title = "The number of records affected or returned by the operation"
        )
        private final Long size;

        @Schema(
            title = "List of IDs for CREATE operations or affected record IDs",
            description = "Contains the IDs of created records (CREATE) or affected records (WRITE/UNLINK)."
        )
        private final List<Integer> ids;
    }
}