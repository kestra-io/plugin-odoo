# How to use the Odoo plugin

Query and modify records in any Odoo model from Kestra flows via XML-RPC.

## Authentication

Set `url` to your Odoo server URL, `db` to the database name, `username`, and `password`. Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

`Query` executes an operation against an Odoo `model` — set `operation` to one of: `SEARCH_READ`, `READ`, `CREATE`, `WRITE`, `UNLINK`, `SEARCH`, or `SEARCH_COUNT`. Pass domain `filters` as a list, scope returned columns with `fields`, supply field values for writes via `values`, and target specific records with `ids`. Paginate with `limit` and `offset`. Control result handling with `fetchType`: `NONE` (default), `FETCH`, `FETCH_ONE`, or `STORE`.
