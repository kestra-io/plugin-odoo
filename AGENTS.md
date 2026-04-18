# Kestra Odoo Plugin

## What

- Provides plugin components under `io.kestra.plugin.odoo`.
- Includes classes such as `Operation`, `OdooClient`, `Query`, `OdooAuthenticator`.

## Why

- This plugin integrates Kestra with Odoo.
- It provides tasks that call the Odoo XML-RPC API for ERP automation.

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
