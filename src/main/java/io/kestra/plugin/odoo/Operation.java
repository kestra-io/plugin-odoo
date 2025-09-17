package io.kestra.plugin.odoo;

/**
 * Enumeration of supported Odoo operations via XML-RPC API.
 */
public enum Operation {
    SEARCH_READ("search_read"),
    READ("read"),
    CREATE("create"),
    WRITE("write"),
    UNLINK("unlink"),
    SEARCH("search"),
    SEARCH_COUNT("search_count");

    private final String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}