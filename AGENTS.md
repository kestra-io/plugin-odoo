# Kestra Odoo Plugin

## What

- Provides plugin components under `io.kestra.plugin.odoo`.
- Includes classes such as `Operation`, `OdooClient`, `Query`, `OdooAuthenticator`.

## Why

- What user problem does this solve? Teams need to call the Odoo XML-RPC API for ERP automation from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Odoo steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Odoo.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `odoo`

Infrastructure dependencies (Docker Compose services):

- `data`
- `db`
- `default`
- `odoo`
- `postgres`

### Key Plugin Classes

- `io.kestra.plugin.odoo.Query`

### Project Structure

```
plugin-odoo/
├── src/main/java/io/kestra/plugin/odoo/
├── src/test/java/io/kestra/plugin/odoo/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
