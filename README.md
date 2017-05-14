# What is it

This branch of the sample Play application is to measure performance 
of three different ways dealing with performing multiple 
sql-operations in one transaction.

 * Async SQL operations run in parallel (on multiple threads available)
 * Async SQL operations run serially (on one thread)
 * Blocking SQL operations (one thread)
 
# How to run the benchmark

You need an empty postgres database created and write access to the database.

By default the application expects to access the database using this config (application.conf):
    
    db.default.username=postgres
    db.default.password=postgres
    db.default.url="jdbc:postgresql://localhost:5432/play_test_rollback_1"
    db.default.driver=org.postgresql.Driver

As soon as the database is created run the application:

    sbt run

And make an http request:

    curl http://localhost:9000/benchmark

**Example:**
    
    $ curl http://localhost:9000/benchmark | jq
    {
      "asyncExecTime": "93.37673950000001",
      "syncExecTime": "69.22052446666666",
      "seqExecTime": "42.45277646666668",
      "sync-speedup": 1.348974747294292,
      "seq-speedup": 2.199543758305611
    }
