# What is it

This is a simple Scala Play application that reproduces transaction rollback issue when one connection shared between multiple threads.

# How does it reproduces the issue

It opens a database connection then runs in parallel a few hundreds of database inserts using this connection.

Every 33rd insert generates an SQL exception so that transaction rollback will be called.

# How to run it

You need an empty postgres database created and write access to the database.

By default the application expects to access the database using this config (application.conf):
    
    db.default.username=postgres
    db.default.password=postgres
    db.default.url="jdbc:postgresql://localhost:5432/play_test_rollback_1"
    db.default.driver=org.postgresql.Driver

As soon as the database is created run the application:

    sbt run

And make an http request:

    curl http://localhost:9000/test

This will cause the multiple parallel inserts with eventual failure and rollback.

The response should contain a json array with table rows that has not been rolled back for some reason.

Sometimes the response is empty. This means that issue did not reveal itself. In this case just make another request. Eventually you will see non empty result which means not all data war rolled back.


**Example:**
    
    $ curl http://localhost:9000/test | python -m json.tool
      % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                     Dload  Upload   Total   Spent    Left  Speed
    100     2    0     2    0     0      0      0 --:--:--  0:00:13 --:--:--     0
    []

    $ curl http://localhost:9000/test | python -m json.tool
      % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                     Dload  Upload   Total   Spent    Left  Speed
    100  1102  100  1102    0     0   5688      0 --:--:-- --:--:-- --:--:--  5709
    [
        {
            "created": "2017-05-09T17:25:30.043Z",
            "created_by": "application-akka.actor.default-dispatcher-9",
            "id": 533,
            "text": "row 48, connection: HikariProxyConnection@2145720414 wrapping org.postgresql.jdbc.PgConnection@778a6e85"
        },
        {
            "created": "2017-05-09T17:25:30.045Z",
            "created_by": "application-akka.actor.default-dispatcher-3",
            "id": 534,
            "text": "row 50, connection: HikariProxyConnection@2145720414 wrapping org.postgresql.jdbc.PgConnection@778a6e85"
        },
        {
            "created": "2017-05-09T17:25:30.045Z",
            "created_by": "application-akka.actor.default-dispatcher-10",
            "id": 535,
            "text": "row 49, connection: HikariProxyConnection@2145720414 wrapping org.postgresql.jdbc.PgConnection@778a6e85"
        },
        {
            "created": "2017-05-09T17:25:30.045Z",
            "created_by": "application-akka.actor.default-dispatcher-5",
            "id": 536,
            "text": "row 53, connection: HikariProxyConnection@2145720414 wrapping org.postgresql.jdbc.PgConnection@778a6e85"
        },
        {
            "created": "2017-05-09T17:25:30.045Z",
            "created_by": "application-akka.actor.default-dispatcher-9",
            "id": 537,
            "text": "row 55, connection: HikariProxyConnection@2145720414 wrapping org.postgresql.jdbc.PgConnection@778a6e85"
        }
    ]
