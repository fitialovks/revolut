# Revolut home task

This is a REST API over an in-memory database of accounts. It allows
creating accounts and moving money between them, to and from external systems.

## Building and running

You can build the application with Gradle:
```
./gradlew installDist
```
This will put application distribution into `./build/install/accountdb/`.
Run the application using following command:
```
./build/install/accountdb/bin/accountdb
```
Stop it with `Ctrl+C`.

You may need a [Lombok](https://projectlombok.org/) plugin for your IDE to view the code.

## Most important classes

[Database](src/main/java/ru/halcraes/revolut/db/Database.java) class is responsible for initializing H2
and updating the schema using Liquibase. It exports a standard `DataSource` for the rest of the application.

[AccountService](src/main/java/ru/halcraes/revolut/db/AccountService.java) wraps all SQL and JDBC
on top of the `DataSource` into a Java API. It uses transactions to make sure the DB stays consistent.

[RestApi](src/main/java/ru/halcraes/revolut/web/RestApi.java) (as you probably guessed)
builds a REST API on top of `AccountService` using Spark.

## Implementation notes

`AccountService` only works with its own transaction id format and does not accept references to
any external systems. This is done to guarantee even performance. Integrations with
external services are expected to store both internal transaction id and external reference
in their own databases. To avoid a call to `/api/v1/transaction/id` (or any other two step contract)
a consumer can still generate its own UUID.

In case an API consumer is unsure if a transaction was complete it can use its stored
transaction id and details to safely repeat the transaction. If all details match
an existing transaction it will get the timestamp of that transaction. If transaction id
does not exist in the DB, the transaction will be executed. Otherwise the request will
return a duplicate transaction id error.

Error handling in `RestApi` can be significantly improved by adding custom exceptions, custom API
like `Preconditions` that throws them and exception handlers in Spark.
Once that's done, it also makes sense to test if REST API actually produces proper
errors. Overall a lot of boring work that is hard to justify in such an exercise.

I may have overdone it with `AccountId` and `TransactionId`, the intention is that they are never mixed
with any other `long` or `UUID`.

After looking for an easy way to integrate Swagger UI I decided to make some `curl` examples instead.

## Usage examples

See some shell scripts in [src/main/dist/bin](src/main/dist/bin). They are also copied into `build/install/accountdb/bin` folder.
