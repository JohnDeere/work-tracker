# The Java Web Servlet Module
This module is intended to be used for plain `Java` projects using the Web Servlet api.

If you are using:
- **Spring**, see this [module](../work-tracker-spring).
- **Spring Boot**, see this [module](../work-tracker-spring-boot)

These Spring modules have more Spring specific metadata.

### Dependencies
See [releases](../../../releases/latest) for the latest release
```xml
<dependency>
    <groupId>com.deere.isg.work-tracker</groupId>
    <artifactId>work-tracker-servlet</artifactId>
    <version>${work-tracker.version}</version>
</dependency>
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>3.1.0</version>
</dependency>
<!-- if you plan to use logback.groovy, use Groovy 2.4.0 or latest -->
<dependency>
    <groupId>org.codehaus.groovy</groupId>
    <artifactId>groovy-all</artifactId>
    <version>2.4.0</version>
</dependency>
```
**Note:** `Logback` dependencies are already included with this library, so there is no need to explicitly include them in your pom.xml.

**Required:** Create a subclass for `WorkContextListener` to initialize your `WorkConfig` (i.e. outstanding, floodSensor and zombie detector):

```java
public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<>(new OutstandingWork<>())
                .withHttpFloodSensor() //Optional, omit if not required
                .withZombieDetector() //Optional, omit if not required
                .build()
        );
    }
}
```

In your web.xml, add the following:
```xml
<!-- filters -->
<filter>
    <filter-name>httpWorkFilter</filter-name>
    <filter-class>com.deere.isg.worktracker.servlet.HttpWorkFilter</filter-class>
</filter>

<filter>
    <filter-name>requestBouncerFilter</filter-name>
    <filter-class>com.deere.isg.worktracker.servlet.RequestBouncerFilter</filter-class>
</filter>

<filter>
    <filter-name>zombieFilter</filter-name>
    <filter-class>com.deere.isg.worktracker.servlet.ZombieFilter</filter-class>
</filter>

<!-- filter mappings -->
<filter-mapping>
    <filter-name>httpWorkFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>

<filter-mapping>
    <filter-name>requestBouncerFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>

<filter-mapping>
    <filter-name>zombieFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>

<!-- listeners -->
<!-- add your workTrackerContextListener -->
<listener>
    <listener-class>com.example.WorkTrackerContextListener</listener-class>
</listener>
```

## Connection Limits (Flood Sensor)
Request Bouncer requires Connection Limits to determine whether to reject a work if the work exceeds a particular limit. 
By default, `ConnectionLimits` provides limits for `same session`, `same user`, `same service` and `total`. 
You can also provide your own limits as follows:

```java
public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<>(MDC.init(new OutstandingWork<>()))
                .setHttpFloodSensorWithLimit(connectionLimits()) //add the connectionLimits here
                .withZombieDetector()
                .build()
        );
    }


    public static ConnectionLimits<HttpWork> connectionLimits() {
        ConnectionLimits<HttpWork> limits = new ConnectionLimits<>();
        //limit, typeName and function
        limits.addConnectionLimit(25, "service").method(SpringWork::getService);
        //limit, typeName and Predicate
        limits.addConnectionLimit(20, "acceptHeader").test(w -> w.getAcceptHeader().contains("xml"));
        //limit, typeName and a dynamic predicate
        limits.addConnectionLimit(10, "acceptHeader")
            .buildTest(incoming -> (incoming.getService().contains("foo") ? 
                (w->incoming.getService().equals(w.getService())) : 
                (w->false)));
        //limit, typeName and function to execute retry later calculation
        limits.addConnectionLimit(2, USER_TYPE).advanced(incoming -> Optional.of(incoming.getElapsedMillis()));
        //limit, typeName, floodSensor and function to execute retry later calculation
        limits.addConnectionLimit(2, USER_TYPE).advanced((floodSensor, incoming) -> Optional.of(incoming.getElapsedMillis()));
        return limits;
    }

}
```

See [example](./../work-tracker-examples/java-example), [web.xml](./../work-tracker-examples/java-example/src/main/webapp/WEB-INF/web.xml)

When a connection limit is tripped, the following happens:
* The client gets an Http Status code of 429 - TOO MANY REQUESTS.
* The client gets a Retry-After header with its value in seconds.  The number of seconds that are given to wait is determined by finding the oldest similar request, 
  and getting the ceiling of the current elapsed time of that request.
* A log statement will be written with the following metadata:
  * message: "Request rejected to protect JVM from too many requests".  There will be additional wording after this statement about what kind of limit was reached.
  * retry_after_seconds: The value given in the Retry-After header to the client.
  * limit_type: The type name of the limit that was tripped.  Standard limit types are: total, user, session, and service.
  * All the other metadata kept track of in the Work bean.
  
  With this metadata you will be able to create dashboards that show who had 429s and why they tripped in an easily digestible format.

## Outstanding HttpServlet
We provide a WorkHttpServlet that displays all the outstanding work that are currently in progress. This can be used for debugging purposes. Below is the configuration in `web.xml`:
```xml
<servlet>
    <servlet-name>workHttpServlet</servlet-name>
    <servlet-class>com.deere.isg.worktracker.servlet.WorkHttpServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>workHttpServlet</servlet-name>
    <url-pattern>/health/outstanding</url-pattern>
</servlet-mapping>
```

## Executor with Metadata
Track your background tasks with the `MdcExecutor`. Example:

```java
// Initialization
// Can use any executor, this is just an example
private ExecutorService service = Executors.newFixedThreadPool(3);
private Executor executor = new MdcExecutor(service);

// Usage
executor.execute(someRunnable);

// Destroy
// an example shutdown
service.shutdown();
try {
    service.awaitTermination(10, TimeUnit.SECONDS);
} catch (InterruptedException e) {
    logger.error("Could not complete task", e);
}
```
