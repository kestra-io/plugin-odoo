package io.kestra.plugin.odoo;

import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * XML-RPC client for communicating with Odoo ERP system.
 * Handles authentication and model operations via Odoo's XML-RPC API.
 */
@Slf4j
public class OdooClient {
    private final String url;
    private final String database;
    private final String username;
    private final String password;
    private final XmlRpcClient objectClient;
    private final OdooAuthenticator authenticator;
    private Integer uid;

    public OdooClient(String url, String database, String username, String password) throws MalformedURLException {
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;

        // Initialize authenticator
        this.authenticator = new OdooAuthenticator(url, database, username, password);

        // Initialize object endpoint client for model operations
        this.objectClient = new XmlRpcClient();
        XmlRpcClientConfigImpl objectConfig = new XmlRpcClientConfigImpl();
        objectConfig.setServerURL(new URL(String.format("%s/xmlrpc/2/object", url)));
        this.objectClient.setConfig(objectConfig);
    }

    /**
     * Authenticate with Odoo and retrieve user ID.
     *
     * @throws Exception if authentication fails
     */
    public void authenticate() throws Exception {
        this.uid = authenticator.authenticate();
    }

    /**
     * Get Odoo server version information.
     *
     * @return Map containing version information
     * @throws Exception if version check fails
     */
    public Map<String, Object> getVersion() throws Exception {
        return authenticator.getVersion();
    }

    /**
     * Execute a model operation using execute_kw.
     *
     * @param model The Odoo model name (e.g., "res.partner")
     * @param method The method to call (e.g., "search_read", "create", "write")
     * @param args The method arguments
     * @param kwargs The method keyword arguments (optional)
     * @return The result of the operation
     * @throws Exception if the operation fails
     */
    public Object executeKw(String model, String method, List<Object> args, Map<String, Object> kwargs) throws Exception {
        if (uid == null) {
            throw new IllegalStateException("Not authenticated. Call authenticate() first.");
        }

        log.debug("Executing {}.{} with args: {}", model, method, args);

        List<Object> params = Arrays.asList(database, uid, password, model, method, args);
        if (kwargs != null && !kwargs.isEmpty()) {
            params = Arrays.asList(database, uid, password, model, method, args, kwargs);
        }

        try {
            Object result = objectClient.execute("execute_kw", params);
            log.debug("Operation {}.{} completed successfully", model, method);
            return result;
        } catch (Exception e) {
            log.error("Failed to execute {}.{}: {}", model, method, e.getMessage());
            throw new Exception(String.format("Failed to execute %s.%s: %s", model, method, e.getMessage()), e);
        }
    }

    /**
     * Search and read records from a model.
     *
     * @param model The Odoo model name
     * @param domain The search domain (list of filter conditions)
     * @param fields The fields to read (optional)
     * @param limit Maximum number of records to return (optional)
     * @param offset Number of records to skip (optional)
     * @return List of records matching the criteria
     * @throws Exception if the operation fails
     */
    @SuppressWarnings("unchecked")
    public Object searchRead(String model, List domain, List<String> fields, Integer limit, Integer offset) throws Exception {
        List<Object> args = Arrays.asList(domain != null ? domain : Collections.emptyList());

        Map<String, Object> kwargs = new java.util.HashMap<>();
        if (fields != null && !fields.isEmpty()) {
            kwargs.put("fields", fields);
        }
        if (limit != null) {
            kwargs.put("limit", limit);
        }
        if (offset != null) {
            kwargs.put("offset", offset);
        }

        return executeKw(model, "search_read", args, kwargs.isEmpty() ? null : kwargs);
    }

    /**
     * Create a new record in a model.
     *
     * @param model The Odoo model name
     * @param values The field values for the new record
     * @return The ID of the created record
     * @throws Exception if the operation fails
     */
    public Object create(String model, Map<String, Object> values) throws Exception {
        List<Object> args = Arrays.asList(values);
        return executeKw(model, "create", args, null);
    }

    /**
     * Update existing records in a model.
     *
     * @param model The Odoo model name
     * @param ids The IDs of records to update
     * @param values The field values to update
     * @return True if the update was successful
     * @throws Exception if the operation fails
     */
    public Object write(String model, List<Integer> ids, Map<String, Object> values) throws Exception {
        List<Object> args = Arrays.asList(ids, values);
        return executeKw(model, "write", args, null);
    }

    /**
     * Delete records from a model.
     *
     * @param model The Odoo model name
     * @param ids The IDs of records to delete
     * @return True if the deletion was successful
     * @throws Exception if the operation fails
     */
    public Object unlink(String model, List<Integer> ids) throws Exception {
        List<Object> args = Arrays.asList(ids);
        return executeKw(model, "unlink", args, null);
    }

    /**
     * Search for record IDs that match the given domain.
     *
     * @param model The Odoo model name
     * @param domain The search domain
     * @param limit Maximum number of IDs to return (optional)
     * @param offset Number of records to skip (optional)
     * @return List of record IDs
     * @throws Exception if the operation fails
     */
    @SuppressWarnings("unchecked")
    public Object search(String model, List domain, Integer limit, Integer offset) throws Exception {
        List<Object> args = Arrays.asList(domain != null ? domain : Collections.emptyList());

        Map<String, Object> kwargs = new java.util.HashMap<>();
        if (limit != null) {
            kwargs.put("limit", limit);
        }
        if (offset != null) {
            kwargs.put("offset", offset);
        }

        return executeKw(model, "search", args, kwargs.isEmpty() ? null : kwargs);
    }

    /**
     * Read specific fields for records with known IDs.
     *
     * @param model The Odoo model name
     * @param ids The IDs of records to read
     * @param fields The fields to read (optional)
     * @return List of records with requested fields
     * @throws Exception if the operation fails
     */
    public Object read(String model, List<Integer> ids, List<String> fields) throws Exception {
        List<Object> args = Arrays.asList(ids);

        Map<String, Object> kwargs = new java.util.HashMap<>();
        if (fields != null && !fields.isEmpty()) {
            kwargs.put("fields", fields);
        }

        return executeKw(model, "read", args, kwargs.isEmpty() ? null : kwargs);
    }

    /**
     * Count records that match the given domain.
     *
     * @param model The Odoo model name
     * @param domain The search domain
     * @return Number of matching records
     * @throws Exception if the operation fails
     */
    @SuppressWarnings("unchecked")
    public Object searchCount(String model, List domain) throws Exception {
        List<Object> args = Arrays.asList(domain != null ? domain : Collections.emptyList());
        return executeKw(model, "search_count", args, null);
    }

    /**
     * Get user ID after authentication.
     *
     * @return The authenticated user ID, or null if not authenticated
     */
    public Integer getUid() {
        return uid;
    }

    /**
     * Check if the client is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return uid != null;
    }
}