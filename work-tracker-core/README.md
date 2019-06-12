## Installation
Before using the core package, see if one of these options above will work for you first.
- **Java Web Projects:** See [Readme](../work-tracker-servlet)
- **Spring Projects:** See [Readme](../work-tracker-spring)
- **Spring Boot Projects:** See [Readme](../work-tracker-spring-boot)

```xml
<dependency>
    <groupId>com.deere.isg.work-tracker</groupId>
    <artifactId>work-tracker-core</artifactId>
    <version>${work-tracker.version}</version>
</dependency>
```

Of course, pin to [the latest released version](./../../../releases/latest).

### Module support for Java 9 and later
`requires com.deere.isg.worktracker.core;`

See [example](../work-tracker-servlet/src/main/java9/module-info.java)
