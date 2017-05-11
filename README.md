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


# Workaround

ConnectionWithThread fixes the issue. 

Basically for each connection it creates an execution context with one thread so that the tread will be used for every sql query on the connection.

*Example:*

If I run 5 requests in parallel:
    
    $ curl http://localhost:9000/test & curl http://localhost:9000/test & curl http://localhost:9000/test & curl http://localhost:9000/test & curl http://localhost:9000/test
    [1] 48227
    [2] 48228
    [3] 48229
    [4] 48230
    [][][][][][1]   Done                    curl http://localhost:9000/test
    [2]   Done                    curl http://localhost:9000/test
    [3]-  Done                    curl http://localhost:9000/test
    [4]+  Done                    curl http://localhost:9000/test

Then in the result we have 5 times empty list `[][][][][]` (which means rollback was successful)

And I can see in logs that each different thread uses single JDBC connection:

    ...    
     application-akka.actor.default-dispatcher-3	904157417015707 				Inserted `row 26` (connection: HikariProxyConnection@1989603 wrapping org.postgresql.jdbc.PgConnection@5206a6db)
     application-akka.actor.default-dispatcher-3	904157417071694 		Try to insert `row 27` (connection: HikariProxyConnection@1989603 wrapping org.postgresql.jdbc.PgConnection@5206a6db)
     application-akka.actor.default-dispatcher-3	904157417513913 				Inserted `row 27` (connection: HikariProxyConnection@1989603 wrapping org.postgresql.jdbc.PgConnection@5206a6db)
     application-akka.actor.default-dispatcher-3	904157417568543 		Try to insert `row 28` (connection: HikariProxyConnection@1989603 wrapping org.postgresql.jdbc.PgConnection@5206a6db)
    ...
     application-akka.actor.default-dispatcher-7	904157482010635 				Inserted `row 22` (connection: HikariProxyConnection@719380029 wrapping org.postgresql.jdbc.PgConnection@2edf4f3e)
     application-akka.actor.default-dispatcher-7	904157482035576 		Try to insert `row 23` (connection: HikariProxyConnection@719380029 wrapping org.postgresql.jdbc.PgConnection@2edf4f3e)
     application-akka.actor.default-dispatcher-6	904157482063374 				Inserted `row 27` (connection: HikariProxyConnection@1576295265 wrapping org.postgresql.jdbc.PgConnection@34370385)
     application-akka.actor.default-dispatcher-6	904157482083309 		Try to insert `row 28` (connection: HikariProxyConnection@1576295265 wrapping org.postgresql.jdbc.PgConnection@34370385)
    ...
     application-akka.actor.default-dispatcher-9	904157483369490 				Inserted `row 17` (connection: HikariProxyConnection@464785022 wrapping org.postgresql.jdbc.PgConnection@1f8b9957)
     application-akka.actor.default-dispatcher-9	904157483384171 		Try to insert `row 18` (connection: HikariProxyConnection@464785022 wrapping org.postgresql.jdbc.PgConnection@1f8b9957)
     application-akka.actor.default-dispatcher-7	904157483655109 				Inserted `row 27` (connection: HikariProxyConnection@719380029 wrapping org.postgresql.jdbc.PgConnection@2edf4f3e)
     application-akka.actor.default-dispatcher-7	904157483685909 		Try to insert `row 28` (connection: HikariProxyConnection@719380029 wrapping org.postgresql.jdbc.PgConnection@2edf4f3e)
    ...
     application-akka.actor.default-dispatcher-7	904157487894131 				Inserted `row 37` (connection: HikariProxyConnection@719380029 wrapping org.postgresql.jdbc.PgConnection@2edf4f3e)
     application-akka.actor.default-dispatcher-7	904157487914611 		Try to insert `row 38` (connection: HikariProxyConnection@719380029 wrapping org.postgresql.jdbc.PgConnection@2edf4f3e)
     application-akka.actor.default-dispatcher-9	904157487976180 				Inserted `row 27` (connection: HikariProxyConnection@464785022 wrapping org.postgresql.jdbc.PgConnection@1f8b9957)
     application-akka.actor.default-dispatcher-9	904157487995453 		Try to insert `row 28` (connection: HikariProxyConnection@464785022 wrapping org.postgresql.jdbc.PgConnection@1f8b9957)
    ...
     application-akka.actor.default-dispatcher-9	904157488562153 		Try to insert `row 30` (connection: HikariProxyConnection@464785022 wrapping org.postgresql.jdbc.PgConnection@1f8b9957)
     application-akka.actor.default-dispatcher-7	904157488507038 		Try to insert `row 40` (connection: HikariProxyConnection@719380029 wrapping org.postgresql.jdbc.PgConnection@2edf4f3e)
     application-akka.actor.default-dispatcher-6	904157488574506 		Try to insert `row 45` (connection: HikariProxyConnection@1576295265 wrapping org.postgresql.jdbc.PgConnection@34370385)
     application-akka.actor.default-dispatcher-10	904157488561300 		Try to insert `row 28` (connection: HikariProxyConnection@575039675 wrapping org.postgresql.jdbc.PgConnection@2679f8ac)
