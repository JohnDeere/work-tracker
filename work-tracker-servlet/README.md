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
    <groupId>com.deere.isg.devops.work-tracker</groupId>
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

## Connection Limits
Request Bouncer requires Connection Limits to determine whether to reject a work if the work exceeds a particular limit. By default, `ConnectionLimits` provides limits for `same session`, `same user`, `same service` and `total`. You can also provide your own limits as follows:

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
        return limits;
    }

}
```

See [example](./../work-tracker-examples/java-example), [web.xml](./../work-tracker-examples/java-example/src/main/webapp/WEB-INF/web.xml)

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
