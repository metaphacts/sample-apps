# Example app for demo of integration of data using SQL service wrapper

This example app demonstrates how the out-of-box SQL service wrapper can be configured to access movie data from the dvdrental Postgres database from metaphactory. The demo is based on a public Postgres tutorial available at https://neon.tech/postgresql/postgresql-getting-started.


The complete example is available in this folder.

## Preparation of Postgres SQL database

The following setup prepares a local Postgres SQL service with data from https://neon.tech/postgresql/postgresql-getting-started. The database is available using JDBC at `jdbc:postgresql://localhost:5432/dvdrental`.

1. Run the Postgres service using the prepared docker compose from the `dvdrental-postgres` folder

```
cd dvdrental-postgres
docker compose up -d
```

2. For the first start make sure to pre-populate the dvdrental data using the provided script

```
docker compose exec postgres-dvdrental /postgres-setup/load-data.sh
```

Note: When no longer needed, the service can be shutdown using `docker compose down` (without deleting the container and volumes) or with `docker compose down -v` (to complete clean the environment).


## Demo

In the demo we access DVD Rental data as described on https://neon.tech/postgresql/postgresql-getting-started through the Ephedra SQL service from metaphactory.

The concrete demo shows action films from the DVD rental database ordered by year.

To achieve this we have defined a SQL service description to map inputs and output from the SPARQL query as parameters in the SQL query.

After deployment the basic example is available on http://localhost:10214/resource/?uri=http://www.example.org/DvdRentalDemo