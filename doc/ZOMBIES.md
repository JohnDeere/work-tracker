# Zombies
## What is a Zombie?
Zombies are those processes running in your application that the user has abandoned.  It is hard to know
for sure that any particular process is a zombie that no one is waiting for.  However, there is often
a timeout in some part of your infrastructure that cuts off the connection, such as a proxy or web server
that has a gateway timeout.  Thus we can at least 
assume that any web traffic that has been going on longer than that is just waste.  It is consuming 
resources.  It is is eating memory (brainz!).  It is costing you money, and could eventually cause 
your server to crash.  It might take hours to crash.  Meanwhile, your other users are starting to feel the 
effects of having fewer resources to do their work on that server as things slowly grind to a halt.

So let's find those Zombies and get rid of them!

## Zombie Detection
The first step in killing zombies is finding them and making them visible. This might be all that you
want to do in your application, especially as you are first adopting work-tracker.

### See zombies in End Logging
The first thing that work-tracker does to point out zombies is that in the 'end' log, it will provide a 
bit of metadata called '`zombie`' that is a boolean. You get this by just enabling the WorkFilter.
That isn't terribly helpful to detect current threats, but at least you'll see it at the end of the request.  
If your system crashes though, you'll likely never see this in your logs. So let's do better!

### See zombies real-time with WorkHttpServlet
The `WorkHttpServlet` servlet can also give you visibility to zombies. Your zombies will show up at the 
top of the report, and when they exceed the timeout, will turn red.

Enable this servlet in 
* [a non-Spring Boot application](../work-tracker-servlet#outstanding-httpservlet)
* [Spring Boot](../work-tracker-spring-boot#outstanding-httpservlet) where it is automatically 
configured.

### See zombies ongoing in your logs
Work-tracker can create a background process that will wake every 30 seconds and create a log entry for each 
ongoing HTTP request that has been living for at least 30 seconds.  When a request has been living more than 
your 'zombie time', it will make the '`zombie`' metadata key true.

To enable this process, add `.withZombieDetector()` to the WorkConfig as follows:
```java
public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<>(new OutstandingWork<Work>(), HttpWork.class)
                .withZombieDetector() // add this to get Zombie logging
                .build()
        );
    }
}
```
The second argument in the Builder constructor (the  HttpWork.class)
### Make a dashboard to see your zombies
You can now use your favorite log aggregator and create a dashboard that will show your zombies in progress.
You can filter by `zombie: true` and count the # of unique `request_id`s.  Split the data by '`service`' or '`endpoint`'
or use a tag cloud to see what kind of code process is causing the zombies.

### Changing when a zombie is detected
Work-tracker assumes that your infrastructure's timeout is 5 minutes. You can override this by calling
```java
Work.setMaxTime(millis);
```

## Zombie Killing
Now that you know your zombies are there, you need a way to do away with them.  Java isn't helpful in this 
regard.  `Thread.stop()` is deprecated, and even if you do use it, it won't work when the thread you are trying
to stop is within a `synchronized` block somewhere within its call stack.  This is nearly always happening
when waiting on I/O, and that is what threads are doing most of the time in a typical application.  The 
`ZombieDetector` does make a valiant effort to stop the thread anyways.  When it works, that's great, but there
does need to be some kind of backup plan.

There are points in the execution of your program that are good checkpoints to see if this request has been 
processing for too long.  One of those is when the application is making another request to a micro service 
dependency.  For Spring Boot, it is pretty easy to configure an 
[Interceptor for RestTemplates](../work-tracker-spring-boot#interceptor-for-resttemplates)
which will check how long the current request has been running and kill it instead of continuing.  If you
don't have a Spring Boot application or just want to use a different checkpoint, call `ZombieDetector.killRunaway()`.

There are a couple of ways to get a hold of a ZombieDetector instance:
* The `WorkContextListener` you set up above creates an attribute in the ServletContext called `zombie_detector`,
which you can get in your application.
* Instead of using `withZombieDetector()` in your `WorkContextListener`, you can configure your own with 
`setZombieDetector(ZombieDetector)` and keep somewhere you can access it in your application such as a 
static variable or a Spring bean.
* When using Spring Boot there is a pre-configured bean called `zombieDetector` that you can inject as a 
dependency wherever you need it. 

## Zombie death accounting
The way that `ZombieDetector.killRunaway()` does its dirty work is that it throws a `ZombieError`.  This
will generally make it through all your catch blocks (unless you have caught Throwable and not rethrown it)
and will usually kill the work pretty effectively.  You still want an appropriate `status_code` to be logged
in your meta data, and just in case someone is still listening to that request, you do want to give them 
some kind of appropriate feedback. This is why we have you configure a `ZombieFilter` in your application. 
The `ZombieFilter` converts these errors into a `504 Gateway Timeout` HTTP status code, and logs the result
appropriately.

Work-tracker-spring-boot's `SpringBootWorkFilter` configures this automatically for you.  If you don't have
Spring Boot, then just configure the following in your web.xml, just after the `WorkFilter`:

```xml
<filter>
    <filter-name>zombieFilter</filter-name>
    <filter-class>com.deere.isg.worktracker.servlet.ZombieFilter</filter-class>
</filter>

<filter-mapping>
    <filter-name>zombieFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

## Preventing zombies
The task of preventing zombies is now up to you. You know about them, so use that logging meta data and those
dashboards to discover why they are taking so long and clean up your app!  Zombies lead to poor customer 
satisfaction.  It is well worth the time to prevent an outbreak.

Happy Hunting!
