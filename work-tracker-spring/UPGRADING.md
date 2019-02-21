
To upgrade from 1.0.0-rc9 or earlier, replace  

```xml
    <servlet>
        <servlet-name>workHttpServlet</servlet-name>
        <servlet-class>com.deere.isg.worktracker.servlet.WorkHttpServlet</servlet-class>
   </servlet>
```

with

```xml
    <servlet>
        <servlet-name>workHttpServlet</servlet-name>
        <servlet-class>com.deere.isg.worktracker.spring.SpringWorkHttpServlet</servlet-class>
   </servlet>
```

in your project's web.xml file.
