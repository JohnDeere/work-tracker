[![Build Status](https://travis-ci.org/JohnDeere/work-tracker.svg?branch=master)](https://travis-ci.org/JohnDeere/work-tracker)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.deere.isg.work-tracker/work-tracker/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.deere.isg.work-tracker/work-tracker)
[![DepShield Badge](https://depshield.sonatype.org/badges/JohnDeere/work-tracker/depshield.svg)](https://depshield.github.io)

# Work Tracker
A library to monitor threads and requests. It provides advanced logging capabilities, and protects the application's JVMs from too many requests and from long running requests that would eventually turn into zombies.

**Features:**
1. Has the ability to log contextual thread metadata to `Elasticsearch` for web requests
2. Checks if the number of threads do not exceed the maximum number of
resources than the JVM permits (i.e. database connections, memory resources, etc.). `RequestBouncer` handles those checks
and allows new threads to proceed only if they are within the `Connection Limits`.  
3. Kills threads that take too long to respond, aka `Zombies`
4. Adds exception names to the logs of faulty requests for tracking bugs better, see `RootCauseTurboFilter`
5. Provides contextual thread metadata for background tasks, see `MdcExecutor`

## Setup
- **Java Web Projects:** See [Readme](./work-tracker-servlet)
- **Spring Projects:** See [Readme](./work-tracker-spring)
- **Spring Boot Projects:** See [Readme](./work-tracker-spring-boot)

## Installation
Before using the core package, see if one of the options above will work for you first.

```xml
<dependency>
    <groupId>com.deere.isg.work-tracker</groupId>
    <artifactId>work-tracker-core</artifactId>
    <version>${work-tracker.version}</version>
</dependency>
```
### Module support for Java 9 and later
`requires com.deere.isg.worktracker.core;`
See [example](./work-tracker-servlet/src/main/java9/module-info.java)

Of course, pin to [the latest released version](./../../releases/latest).

## Whitelisting Jobs
If you expect a job to take longer than 5 minutes (i.e. uploading/downloading a file), you may want to `whitelist` that job. Use `Work#setMaxTime(long)` to update the time for Zombie detection.

## Logback features specific to this library
- [RootCauseTurboFilter](./work-tracker-core/src/main/java/com/deere/isg/worktracker/RootCauseTurboFilter.java)

This turbo filter adds the class name of the `root cause` and the `cause` to the MDC when an exception occurs. Those class names will exist until the request ends to provide an easy way to trace down a faulty request from the beginning of the exception to the end of that request.
```groovy
turboFilter(RootCauseTurboFilter)
```

Optional configuration for field names:
```groovy
//...
turboFilter(RootCauseTurboFilter) {
  causeFieldName = "cause_field_name" // Optional, if you want some other value for causeFieldName
  rootCauseFieldName = "root_cause_field_name" // Optional, if you want some other value for rootCauseFieldName
}

//...
```

- [MdcThreadNameJsonProvider](./work-tracker-core/src/main/java/com/deere/isg/worktracker/MdcThreadNameJsonProvider.java)

This JSON Provider is **necessary** if `ZombieDetector` is used. This provider gets the `thread name` of the actual zombie work instead of the `ZombieLogger` class. Thus, making it easier to find out what work was zombie

**Configuration:**
```groovy
encoder(LoggingEventCompositeJsonEncoder) {
   providers(LoggingEventJsonProviders) {
      //...
      mdc(MdcJsonProvider) {
         excludeMdcKeyName = 'thread_name' // Avoid the need to overwrite by threadName()
      }
      threadName(MdcThreadNameJsonProvider)
      //...
  }
}
```

See [example](./work-tracker-examples/java-example/src/main/resources/logback.groovy)

---

## Logging Utilities
### Using Logger to log information
```java
private Logger logger = LoggerFactory.getLogger(CurrentClass.class);
//...
logger.info("This is an info log"); // log some information
logger.warn("This is a warn log"); // log a warning

try {
  //...
} catch(Exception e){
  logger.error("This is an error log", e); // log an error
}
```

### Using `putInContext` to add context to logs
`putInContext` is a wrapper for the `MDC (Mapped Diagnostic Context)`. It puts the key-value pair in the MDC and in the current work payload, thus, making sure that the `ZombieDetector` will have all the work metadata to log to Elasticsearch in the case where you need to track and debug your application for those Zombies.

If you want some context variables to persist for a given request across multiple logs, use the `putInContext` to add those context variables. For example:
```java
// Use the OutstandingWork to put the values to the MDC for the current work
outstandingWork.putInContext("some_id", "some value");
```

**Retrieving the OutstandingWork in the `Java Module` (i.e. using `HttpServlet`)**
```java
// Get the OutstandingWork from the Servlet Context
@Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    OutstandingWork<?> outstandingWork = (OutstandingWork<?>) request
        .getServletContext().getAttribute(OUTSTANDING_ATTR);
    //... add your values to the context using putInContext
}
```

To make life simpler, you can also have a utility class to put the context in the outstandingWork. See [example](./work-tracker-examples/java-example/src/main/java/com/deere/example/MDC.java), and its [initialization](./work-tracker-examples/java-example/src/main/java/com/deere/example/WorkTrackerContextListener.java#)

**Retrieving the OutstandingWork in the `Spring Module`**
```java
// Using field injection
@Autowired
private OutstandingWork<? extends SpringWork> outstandingWork;
//...
```
```java
// OR using constructor injection (use either one, but not both)
private OutstandingWork<? extends SpringWork> outstandingWork;
//...
public SomeClass(@Autowired OutstandingWork<? extends SpringWork> outstandingWork) {
  this.outstandingWork = outstandingWork;
}
```

Now, each of your subsequent logs will have those values in the MDC, unless you clear the MDC (and this library automatically clears the MDC at the end of each request to avoid having stale context).

### Using Structured Arguments for individual log context
If some context variables are specific to a single log (i.e. the sender id for a certain message, elapsed time, etc.), use `StructuredArguments.keyValue()` to add those context variables. Those variables will not persist across all the logs but they will be available for that log only. For example:
```java
logger.info("{} sent a message", keyValue("sender_id", "some sender value"));
```

`keyValue` will turn the message into `"sender_id=some sender value sent a message"`, while also creating an index in Elasticsearch for `sender_id` to make it easier to search for a particular sender_id in Kibana.

**Note:** Make sure that the `key` for `MDC` and `Structured Arguments` is in `snake_case (and lower case)` as suggested by Elasticsearch's [Naming Conventions](https://www.elastic.co/guide/en/beats/devguide/current/event-conventions.html). These naming conventions make it easy to search for those keys in `Kibana` as the keys will be optimized by Elastic Search.


---

## Contributing to this library
Please see the [Contribution Guidelines](./.github/CONTRIBUTING.md).

### Running tests
```bash
mvn clean verify
```

### Running example projects
Don't know how to start, have a look at these [examples](./work-tracker-examples)


## Bump Version For Release
Run the following bash command and commit the change:
```bash
bash build/bump_version.sh MAJOR|MINOR|PATCH
```   

Example:
```bash
bash build/bump_version.sh MINOR
```
