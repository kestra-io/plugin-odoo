<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png"  alt="Kestra workflow orchestrator" />
  </a>
</p>

<h1 align="center" style="border-bottom: none">
    Event-Driven Declarative Orchestrator
</h1>

<div align="center">
 <a href="https://github.com/kestra-io/kestra/releases"><img src="https://img.shields.io/github/tag-pre/kestra-io/kestra.svg?color=blueviolet" alt="Last Version" /></a>
  <a href="https://github.com/kestra-io/kestra/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/kestra-io/kestra?color=blueviolet" alt="License" /></a>
  <a href="https://github.com/kestra-io/kestra/stargazers"><img src="https://img.shields.io/github/stars/kestra-io/kestra?color=blueviolet&logo=github" alt="Github star" /></a> <br>
<a href="https://kestra.io"><img src="https://img.shields.io/badge/Website-kestra.io-192A4E?color=blueviolet" alt="Kestra infinitely scalable orchestration and scheduling platform"></a>
<a href="https://kestra.io/slack"><img src="https://img.shields.io/badge/Slack-Join%20Community-blueviolet?logo=slack" alt="Slack"></a>
</div>

<br />

<p align="center">
    <a href="https://twitter.com/kestra_io"><img height="25" src="https://kestra.io/twitter.svg" alt="twitter" /></a> &nbsp;
    <a href="https://www.linkedin.com/company/kestra/"><img height="25" src="https://kestra.io/linkedin.svg" alt="linkedin" /></a> &nbsp;
<a href="https://www.youtube.com/@kestra-io"><img height="25" src="https://kestra.io/youtube.svg" alt="youtube" /></a> &nbsp;
</p>

<br />
<p align="center">
    <a href="https://go.kestra.io/video/product-overview" target="_blank">
        <img src="https://kestra.io/startvideo.png" alt="Get started in 4 minutes with Kestra" width="640px" />
    </a>
</p>
<p align="center" style="color:grey;"><i>Get started with Kestra in 4 minutes.</i></p>


# Kestra Odoo Plugin

> Integrate with Odoo ERP system via XML-RPC API

This plugin allows Kestra to interact with [Odoo](https://www.odoo.com/) ERP systems using the XML-RPC API. It provides tasks for querying, creating, updating, and deleting records in Odoo models, enabling seamless integration between your data workflows and Odoo business processes.

## Features

- **Query Odoo Models**: Search and read records from any Odoo model using flexible domain filters
- **Create Records**: Create new records in Odoo models with custom field values
- **Update Records**: Modify existing records by ID with new field values
- **Delete Records**: Remove records from Odoo models
- **Count Records**: Get the count of records matching specific criteria
- **Flexible Authentication**: Secure connection using username/password authentication
- **Dynamic Properties**: Support for Kestra's templating engine in all parameters

## Supported Operations

| Operation | Description | Required Parameters |
|-----------|-------------|-------------------|
| `search_read` | Search and retrieve records | `model`, optional: `filters`, `fields`, `limit`, `offset` |
| `create` | Create new records | `model`, `values` |
| `write` | Update existing records | `model`, `ids`, `values` |
| `unlink` | Delete records | `model`, `ids` |
| `search` | Get record IDs matching criteria | `model`, optional: `filters`, `limit`, `offset` |
| `search_count` | Count records matching criteria | `model`, optional: `filters` |

## Quick Start

### Basic Configuration

All tasks require these basic connection parameters:

```yaml
url: https://your-odoo-instance.com  # Odoo server URL
db: your_database                    # Database name
username: your_username              # Odoo username
password: your_password              # Odoo password
model: res.partner                   # Odoo model to operate on
```

### Example: Query Partners

```yaml
id: query_partners
type: io.kestra.plugin.odoo.Query
url: https://my-odoo.com
db: production
username: api_user
password: "{{ secret('ODOO_PASSWORD') }}"
model: res.partner
operation: search_read
filters:
  - ["is_company", "=", true]
  - ["customer_rank", ">", 0]
fields: ["name", "email", "phone", "country_id"]
limit: 100
```

### Example: Create New Customer

```yaml
id: create_customer
type: io.kestra.plugin.odoo.Query
url: https://my-odoo.com
db: production
username: api_user
password: "{{ secret('ODOO_PASSWORD') }}"
model: res.partner
operation: create
values:
  name: "Acme Corporation"
  email: "contact@acme.com"
  is_company: true
  customer_rank: 1
  supplier_rank: 0
```

### Example: Update Records

```yaml
id: update_partners
type: io.kestra.plugin.odoo.Query
url: https://my-odoo.com
db: production
username: api_user
password: "{{ secret('ODOO_PASSWORD') }}"
model: res.partner
operation: write
ids: [1, 2, 3]
values:
  category_id: [[6, 0, [1, 2]]]  # Odoo many2many format
  active: true
```

## Installation

Add this plugin to your Kestra instance:

```bash
./kestra plugins install io.kestra.plugin:plugin-odoo:LATEST
```

## Development

### Prerequisites
- Java 21
- Docker

### Running tests
```bash
./gradlew check --parallel
```

### Local Development

**VSCode**: Follow the README.md within the `.devcontainer` folder for development setup.

**Other IDEs**:
```bash
./gradlew shadowJar && docker build -t kestra-odoo . && docker run --rm -p 8080:8080 kestra-odoo server local
```

Visit http://localhost:8080 to test your plugin.

## Documentation
* Full documentation can be found under: [kestra.io/docs](https://kestra.io/docs)
* Documentation for developing a plugin is included in the [Plugin Developer Guide](https://kestra.io/docs/plugin-developer-guide/)


## License
Apache 2.0 © [Kestra Technologies](https://kestra.io)


## Stay up to date

We release new versions every month. Give the [main repository](https://github.com/kestra-io/kestra) a star to stay up to date with the latest releases and get notified about future updates.

![Star the repo](https://kestra.io/star.gif)
