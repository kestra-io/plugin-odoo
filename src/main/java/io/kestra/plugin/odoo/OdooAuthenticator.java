package io.kestra.plugin.odoo;

import io.kestra.core.exceptions.KestraRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

/**
 * Handles authentication with Odoo ERP system via XML-RPC.
 */
@Slf4j
public class OdooAuthenticator {
    private final String url;
    private final String database;
    private final String username;
    private final String password;
    private final XmlRpcClient commonClient;

    public OdooAuthenticator(String url, String database, String username, String password) throws MalformedURLException {
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;

        // Initialize common endpoint client for authentication
        this.commonClient = new XmlRpcClient();
        XmlRpcClientConfigImpl commonConfig = new XmlRpcClientConfigImpl();
        commonConfig.setServerURL(new URL(String.format("%s/xmlrpc/2/common", url)));
        this.commonClient.setConfig(commonConfig);
    }

    /**
     * Authenticate with Odoo and return the user ID.
     *
     * @return user ID if authentication successful
     * @throws Exception if authentication fails
     */
    public int authenticate() throws Exception {
        log.debug("Authenticating with Odoo server: {}", url);

        try {
            Object result = commonClient.execute("authenticate", Arrays.asList(
                database, username, password, Collections.emptyMap()
            ));

            if (result instanceof Number) {
                int uid = ((Number) result).intValue();
                log.debug("Authentication successful, user ID: {}", uid);
                return uid;
            }

            throw new KestraRuntimeException("Unexpected authentication response from Odoo: " + result);

        } catch (Exception e) {
            log.error("Authentication failed for user '{}' on database '{}': {}", username, database, e.getMessage(), e);
            throw new KestraRuntimeException("Failed to authenticate with Odoo: " + e.getMessage(), e);
        }
    }

    /**
     * Get Odoo server version information.
     *
     * @return Map containing version information
     * @throws Exception if version check fails
     */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> getVersion() throws Exception {
        try {
            Object result = commonClient.execute("version", Collections.emptyList());
            return (java.util.Map<String, Object>) result;
        } catch (Exception e) {
            log.error("Failed to get version info: {}", e.getMessage());
            throw new Exception("Failed to get Odoo version: " + e.getMessage(), e);
        }
    }
}