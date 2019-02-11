# The Spring Boot Module
This module is intended to be used for `Spring Boot` projects.
If you are using:
- **Java with Web Servlet**, see this [module](../work-tracker-servlet).
- **Spring**, see this [module](../work-tracker-spring)

## Dependencies
See [releases](../../../releases/latest) for the latest release
```xml
<dependencies>
    <dependency>
        <groupId>com.deere.isg.work-tracker</groupId>
        <artifactId>work-tracker-spring-boot</artifactId>
        <version>${work-tracker.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>1.5.9.RELEASE</version>
    </dependency>
    <!-- if you plan to use logback.groovy, use Groovy 2.4.0 or latest -->
    <dependency>
        <groupId>org.codehaus.groovy</groupId>
        <artifactId>groovy-all</artifactId>
        <version>2.4.0</version>
    </dependency>
</dependencies>
```

**Note:** `Logback` dependencies are already included with this library, so there is no need to explicitly include them in your pom.xml.

## Spring `@Configuration`
We provide a default configurer `WorkTrackerConfigurer` that your application can extend and override any beans necessary. Below is a basic example:

**NOTE - You are required to:**
1. provide the `SpringWork` class, or a derivative of that class if you plan to have
user authentication in your application, as the `Type` to `WorkTrackerConfigurer`
2. override `workFactory` to provide a [`Function`](https://docs.oracle.com/javase/8/docs/api/java/util/function/Function.html) of how to instantiate that `Type`

```java
@Configuration
public class WorkTrackerConfig extends WorkTrackerConfigurer<SpringWork> {

   @Override
   public Function<ServletRequest, SpringWork> workFactory() {
       return SpringWork::new;
   }
}
```

## Application with User Authentication
You are required to provide a derivative of `SpringWork` to the `WorkTrackerConfigurer`. This `SpringWork` subclass should override `SpringWork#updateUserInformation(HttpServletRequest request)` to add the user's
username to the `remoteUser` by using `Work#setRemoteUser(String)`. You can also add other information in the `MDC` if you intend to use it as context by using `Work#addToMDC(String)`. Example (i.e. using `spring-boot-starter-security`):

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

Then for your `WorkTrackerConfigurer`:
```java
@Configuration
public class WorkTrackerConfig extends WorkTrackerConfigurer<UserSpringWork> {

    @Override
    public Function<ServletRequest, UserSpringWork> workFactory() {
        return UserSpringWork::new;
    }
}
```

See [example](./../work-tracker-examples/spring-boot-example/src/main/java/com/deere/example/spring)

## Key Cleanser
Cleanse your metadata keys before adding them to the MDC by using the `KeyCleanser` interface. It provides you with the key, value, and the URI of the current request. `PathMetadataCleanser` is the default `KeyCleanser`.

**NOTE:** `PathMetadataCleanser` converts every key into `snake_case` since `Work#addToMDC` accepts only snake_case values. It is also provided, by default, in the `WorkTrackerConfigurer` as a bean.

`PathMetadataCleanser` has four steps: Reserved, Standard, Transform, and Banned:
- `Reserved` is for the metadata that are already used in work-tracker and simple words that aren't good context keys. If the key falls in this category, the cleanser will add a context to the key from the path. Example: requests for the URI `/user/{id}` will become `user_id`
- `Standard` is for **converting keys with non-standard names into standardized keys**. Example: `comp_name` can become `complete_name`.  Add to this list using PathMetaDataCleanser.addStandard().
- `Transform` is a function you provide to convert keys that require more than just simple replacement, or apply a rule your application needs to apply to all keys.
- `Banned` is for **blacklisting keys**. Some keys are not allowed in `Elasticsearch` since they are reserved and would cause failure if they are uploaded to Elasticsearch. Banned keys will convert those keys in order to prevent Elasticsearch log upload failures. You can also use it to convert restricted keys into different ones.

Override `keyCleanser` in `WorkTrackerConfigurer` for adding more limits:
```java
@Configuration
public class WorkTrackerConfig extends WorkTrackerConfigurer<SpringWork> {

    @Override
    public KeyCleanser keyCleanser() {
        PathMetadataCleanser cleanser = new PathMetadataCleanser();
        cleanser.addStandard("short_id", "standardized_id");
        cleanser.setTransformFunction(key -> key + "_suffix");
        cleanser.addBanned("banned", "good_id");
        return cleanser;
    }
//...    
}
```

## Connection Limits
Request Bouncer requires Connection Limits to determine whether to reject a work if the work exceeds a particular limit. By default, `ConnectionLimits` provides limits for `same session`, `same user`, `same service` and `total`.

### Automatic Limit detection
The `WorkTrackerConfigurer` can automatically get the max connection limit from your `DataSource`, which can be any database. It looks for the `dataSource` bean, if none is found or if the limit is less than 10, `RequestBouncer` will not be activated. The name for the `dataSource` bean is configurable using `setDataSourceName`. Example:
```java
@Configuration
public class WorkTrackerConfig extends WorkTrackerConfigurer<SpringWork> {
    public WorkTrackerConfig() {
      setDataSourceName("sqlDataSource");
    }
    //...
}
```

### Manual Limit
Another way of configuring the connectionLimits is by setting the limit. This will initialize the connectionLimits with the default types ( `same session`, `same user`, `same service`,  `total`). If the limit is less than 10, `connectionLimits` will not be initialized. Example:
```java
@Configuration
public class WorkTrackerConfig extends WorkTrackerConfigurer<SpringWork> {
    public WorkTrackerConfig() {
      setLimit(60);
    }
    //...
}
```
See `ConnectionLimits` for implementation details

### More Limits
You can also provide your own limits as follows:

```java
@Configuration
public class WorkTrackerConfig extends WorkTrackerConfigurer<SpringWork> {

    @Override
    public ConnectionLimits<SpringWork> connectionLimits() {
        ConnectionLimits<SpringWork> limits = new ConnectionLimits<>();
        // Or ConnectionLimits<SpringWork> limits = super.connectionLimits(); //to get limits from DataSource

        //Add more limits, if necessary
        //First way: limit, typeName and function
        limits.addConnectionLimit(25, "service").method(SpringWork::getService);
        //Second way: limit, typeName and Predicate
        limits.addConnectionLimit(20, "acceptHeader").test(w -> w.getAcceptHeader()
                .contains(MediaType.APPLICATION_XML_VALUE)
        );
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
    //...
}
```
## Outstanding HttpServlet
We provide a WorkHttpServlet that outputs all the outstanding work that are currently in progress. This can be used for debugging purposes. By default, the uri is `/health/outstanding` and is provided by the `WorkTrackerConfigurer`;

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

```java
// Add these beans in your Configuration
@Bean
public TaskExecutor taskExecutor() {
  // Can use any executor, this is just an example
   ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
   executor.setCorePoolSize(3);
   executor.setMaxPoolSize(5);
   executor.initialize();
   return executor;
}

@Bean
public Executor mdcTaskExecutor(@Qualifier("taskExecutor") TaskExecutor executor) {
   return new MdcExecutor(executor);
}

// Use it with Autowiring
@Autowiring
private MdcExecutor mdcExecutor;
//...
mdcExecutor.execute(someRunnable);
```
## Testing

Start with this to test-drive adding in the work-tracker to your application.

```java
import com.deere.isg.worktracker.servlet.WorkConfig;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class ApplicationWorkTrackerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @SpyBean
    private WorkConfig<?> workConfig;

    @Test
    public void basicConfigurationTest() {
        assertThat("Uses work tracking", workConfig.getOutstanding(), notNullValue());
        assertThat("Detects Zombies", workConfig.getDetector(), notNullValue());
        assertThat("Has DoS protection", workConfig.getFloodSensor(), notNullValue());
    }

    @Test
    public void endToEndTest() {
        String body = this.restTemplate.getForObject("/health/outstanding", String.class);
        assertThat(body, Matchers.not(Matchers.containsString("\"status\":500")));
        assertThat(body, Matchers.containsString("<td>GET /health/outstanding</td>"));
    }
}
```
