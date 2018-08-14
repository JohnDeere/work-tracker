# The Spring Module
This module is intended to be used for `Spring` projects.
If you are using:
- **Java with Web Servlet**, see this [module](../work-tracker-servlet).
- **Spring Boot**, see this [module](../work-tracker-spring-boot)

## Dependencies
See [releases](../../../releases/latest) for the latest release
```xml
<dependencies>
  <dependency>
    <!-- add this dependency before work-tracker to avoid Logback dependency conflicts -->
      <groupId>org.logback-extensions</groupId>
      <artifactId>logback-ext-spring</artifactId>
      <version>0.1.4</version>
  </dependency>
  <dependency>
    <groupId>com.deere.isg.devops.work-tracker</groupId>
    <artifactId>work-tracker-spring</artifactId>
    <version>${work-tracker.version}</version>
  </dependency>
  <!-- Spring libraries -->
  <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-web</artifactId>
      <version>${spring.version}</version>
  </dependency>
  <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-webmvc</artifactId>
      <version>${spring.version}</version>
  </dependency>
  <dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>javax.servlet-api</artifactId>
      <version>${servlet.version}</version>
      <scope>provided</scope>
  </dependency>
  <!-- logging libraries and bridges, ref: https://www.slf4j.org/legacy.html -->
  <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>slf4j-api</artifactId>
      <version>${logging.version}</version>
  </dependency>
  <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>jcl-over-slf4j</artifactId>
      <version>${logging.version}</version>
  </dependency>
  <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>jul-to-slf4j</artifactId>
      <version>${logging.version}</version>
  </dependency>
  <dependency>
      <groupId>org.slf4j</groupId>
      <artifactId>log4j-over-slf4j</artifactId>
      <version>${logging.version}</version>
  </dependency>
  <!-- If you plan to use logback.groovy, use Groovy 2.4.0 or latest -->
  <dependency>
      <groupId>org.codehaus.groovy</groupId>
      <artifactId>groovy-all</artifactId>
      <version>${groovy.version}</version>
  </dependency>
</dependencies>
```

See [example](./../work-tracker-examples/spring-example) for more details

## Configuration
**Create a `ContextListener` for WorkTracker**
```java
@Configuration
public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<SpringWork>(new OutstandingWork<>())
                .withHttpFloodSensor() // omit if not needed
                .withZombieDetector() // omit if not needed
                .build());
    }
}
```

If you don't need `Flood Sensor` and/or `Zombie` protection, you can omit `withHttpFloodSensor` and/or `withZombieDetector` to remove those features. You will also need to remove their respective filters from `web.xml`.

**Add the filters and listeners in `web.xml`**
```xml
<context-param>
    <param-name>logbackConfigLocation</param-name>
    <param-value>classpath:logback.groovy</param-value>
</context-param>

<filter>
    <filter-name>springWorkFilter</filter-name>
    <filter-class>com.deere.isg.worktracker.spring.SpringWorkFilter</filter-class>
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
    <filter-name>springWorkFilter</filter-name>
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
<listener>
    <listener-class>ch.qos.logback.ext.spring.web.LogbackConfigListener</listener-class>
</listener>

<listener>
    <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>
<!-- add your workTrackerContextListener -->
<listener>
    <listener-class>com.example.WorkTrackerContextListener</listener-class>
</listener>
```

**Add the interceptors in `applicationContext.xml`**
```xml
<mvc:interceptors>
    <mvc:interceptor>
        <mvc:mapping path="/**"/>
        <bean class="com.deere.isg.worktracker.spring.SpringLoggerHandlerInterceptor"/>
    </mvc:interceptor>
    <mvc:interceptor>
        <mvc:mapping path="/**"/>
        <bean class="com.deere.isg.worktracker.spring.SpringRequestBouncerHandlerInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
```

## Application with User Authentication
Provide a `SpringWork` subclass that overrides `SpringWork#updateUserInformation(HttpServletRequest request)` to add the user's
username to the `remoteUser` using `Work#setRemoteUser(String)`. You can also add other information in the `MDC`, if you intend to use it as context, by using `Work#addToMDC(String)`. Example:

**WARNING:** Please do not add any **password** to the `MDC`.

```java
public class UserSpringWork extends SpringWork {
    public UserSpringWork(ServletRequest request) {
        super(request);
    }

    @Override
    public void updateUserInformation(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        setRemoteUser(auth.getName()); FloodSensor
    }
}
```

Because of Java Type Erasure, you should define a custom `WorkFilter` to take the `UserSpringWork` and discard `SpringWorkFilter` in your `web.xml` in favor of `WorkFilter`:

```java
public class WorkFilter extends AbstractSpringWorkFilter<UserSpringWork> {
    @Override
    protected UserSpringWork createWork(ServletRequest request) {
        return new UserSpringWork(request);
    }
}
```

```xml
<filter>
    <filter-name>workFilter</filter-name>
    <filter-class>com.example.WorkFilter</filter-class>
</filter>
<!--... -->
<filter-mapping>
   <filter-name>workFilter</filter-name>
   <url-pattern>/*</url-pattern>
</filter-mapping>
```

Then add the `SpringWorkPostAuthFilter` to the filter list in `web.xml` after the login/Spring Security filters.

```xml
<filter>
  <!-- Add this after the spring security filters, after the username is known -->
   <filter-name>springWorkPostAuthFilter</filter-name>
   <filter-class>com.deere.isg.worktracker.spring.SpringWorkPostAuthFilter</filter-class>
</filter>
<!--... -->
<filter-mapping>
   <filter-name>springWorkPostAuthFilter</filter-name>
   <url-pattern>/*</url-pattern>
</filter-mapping>
```

Then your configuration will need `UserSpringWork` as the type, example:
```java
@Configuration
public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<UserSpringWork>(new OutstandingWork<>())
            .withHttpFloodSensor() // omit if not needed
            .withZombieDetector() // omit if not needed
            .build());
        );
    }
}
```

### Spring Filter Autowiring
Often times, you have a helper class that you can autowire to retrieve the username. In that case, you can define the `UserSpringWork` class as a component with prototype scope, as follows:
```java
@Component
@Scope("prototype")
public class UserSpringWork extends SpringWork {
  //...
}
```

Then define `SpringWorkPostAuthFilter` as a bean and use `DelegatingFilterProxy` when declaring the filter in `web.xml` with `targetFilterLifecycle` set to `true`. This way `SpringWorkPostAuthFilter` will have Spring context.

```xml
 <bean id="springWorkPostAuthFilter" class="com.deere.isg.worktracker.spring.SpringWorkPostAuthFilter"/>
 <!-- or use @Bean for Java config -->
 ```

```xml
<filter>
    <filter-name>springWorkPostAuthFilter</filter-name>
    <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
    <init-param>
        <param-name>targetFilterLifecycle</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>
<!--...-->
<filter-mapping>
    <filter-name>springWorkPostAuthFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

## Key Cleanser
Cleanse your metadata keys before adding them to the MDC by using the `KeyCleanser` interface. It provides you with the key, value, and the URI of the current request. `PathMetadataCleanser` is the default `KeyCleanser`.

**NOTE:** `PathMetadataCleanser` converts every key into `snake_case` since `Work#addToMDC` accepts only snake_case values.

`PathMetadataCleanser` has four steps: Reserved, Standard, Transform, and Banned:
- `Reserved` is for the metadata that are already used in work-tracker and simple words that aren't good context keys. If the key falls in this category, the cleanser will add a context to the key from the path. Example: requests for the URI `/user/{id}` will become `user_id`
- `Standard` is for **converting keys with non-standard names into standardized keys**. Example: `comp_name` can become `complete_name`.  Add to this list using PathMetaDataCleanser.addStandard().
- `Transform` is a function you provide to convert keys that require more than just simple replacement, or apply a rule your application needs to apply to all keys.
- `Banned` is for **blacklisting keys**. Some keys are not allowed in `Elasticsearch` since they are reserved and would cause failure if they are uploaded to Elasticsearch. Banned keys will convert those keys in order to prevent Elasticsearch log upload failures. You can also use it to convert restricted keys into different ones.

```java
public KeyCleanser keyCleanser() {
    PathMetadataCleanser cleanser = new PathMetadataCleanser();
    cleanser.addStandard("short_id", "starndardized_id");
    cleanser.setTransformFunction(key -> key + "_suffix");
    cleanser.addBanned("banned", "good_id");
    return cleanser;
}
```

Then add it to your WorkFilter

## Connection Limits
Request Bouncer requires Connection Limits to determine whether to reject a work if the work exceeds a particular limit. By default, `ConnectionLimits` provides limits for `same session`, `same user`, `same service` and the `total`. You can also provide your own limits as follows:

```java
@Configuration
public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<SpringWork>(new OutstandingWork<>())
                .setHttpFloodSensorWithLimit(connectionLimits()) //add the connectionLimits here
                .withZombieDetector()
                .build());
    }

    public static ConnectionLimits<SpringWork> connectionLimits() {
        ConnectionLimits<SpringWork> limits = new ConnectionLimits<>();
        //limit, typeName and function
        limits.addConnectionLimit(25, "service").method(SpringWork::getService);
        //limit, typeName and Predicate
        limits.addConnectionLimit(20, "acceptHeader").test(w -> w.getAcceptHeader()
                .contains(MediaType.APPLICATION_XML_VALUE)
        );
        return limits;
    }
}
```

## Extra Features
- **Interceptor for RestTemplates**

We provide an `HttpInterceptor` that has Zombie protection for runaway requests (i.e. requests that can take too long and eventually become orphan because either they never return a value or the user has interrupted the request). This interceptor can be used with a `RestTemplate` or a similar database template in  Spring. Add the following into your configuration class:

```java
@Bean
public RestTemplate restTemplate() {
    RestTemplate template = new RestTemplate();
    template.getInterceptors().add(new ZombieHttpInterceptor());
    return template;
}
```

- **MdcExecutor**

Track your background tasks with the `MdcExecutor`. Example:
```xml
<!-- in applicationContext.xml, add the following -->
<!-- any executor customization is permitted -->
<task:executor id="executor" pool-size="20"/>
```

```java
// Add a bean to your Configuration
@Bean
public Executor mdcExecutor(Executor executor) {
    return new MdcExecutor(executor);
}

// Use it with Autowiring
@Autowiring
private MdcExecutor mdcExecutor;
//...
mdcExecutor.executor(someRunnable);
```

## Outstanding HttpServlet
We provide a WorkHttpServlet that outputs all the outstanding work that are currently in progress. This can be used for debugging purposes. Below is the configuration in `web.xml`:
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

## Helpers
Helper class for autowiring OutstandingWork, ZombieDetector and HttpFloodSensor:

```java
@Configuration
public class WorkContext implements ServletContextAware {
    private OutstandingWork<SpringWork> outstanding;
    private HttpFloodSensor<SpringWork> floodSensor;
    private ZombieDetector detector;

    @Override
    @SuppressWarnings("unchecked")
    public void setServletContext(ServletContext servletContext) {
        this.outstanding = (OutstandingWork<SpringWork>) servletContext.getAttribute(OUTSTANDING_ATTR);
        this.floodSensor = (HttpFloodSensor<SpringWork>) servletContext.getAttribute(FLOOD_SENSOR_ATTR);
        this.detector = (ZombieDetector) servletContext.getAttribute(ZOMBIE_ATTR);
    }

    @Bean
    public OutstandingWork<SpringWork> outstanding() {
        return outstanding;
    }

    @Bean
    public ZombieDetector detector() {
        return detector;
    }

    @Bean
    public HttpFloodSensor<SpringWork> floodSensor() {
        return floodSensor;
    }
}
```
Then using `WorkContext`:
```java
//field injection. Can also use Constructor injection
@Autowired
private OutstandingWork<SpringWork> outstanding;
outstanding.putInContext("some_key", "some value");
//...
@Autowired
private ZombieDetector detector;
detector.killRunaway();
//...
```
