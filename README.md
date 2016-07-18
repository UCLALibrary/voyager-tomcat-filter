# voyager-tomcat-filter
Provides a filter for the Voyager Tomcat OPAC to defend against aggressive harvesters

### Specifications
* Monitor requests for certain URL patterns used for harvesting (and other, legitimate purposes)
* If an IP address makes more than the allowed number of requests during a time interval, temporarily ban (reject) that IP address for further such requests for a period.
  * Number of requests: 20
  * Interval: 10 seconds
  * Duration of ban: 60 seconds
* If a banned IP address makes a request:
  * Reject it via HTTP 403 (Forbidden) response
  * Extend the ban by resetting the starting point to the time of the latest request
* If a banned IP address refrains from making requests during the ban period, its next (post-ban) requests will be allowed as long as they do not re-violate the rules.
* IP addresses which are banned multiple times may be penalized further by increasingly long bans (e.g., 2nd ban lasts 2 minutes, 5th ban lasts 5 minutes, etc.).

### Installation
A very simple build script is provided: `build_filter.sh`.  It's customized for our local environment but should be easy to change for yours.

Tomcat filters are enabled and configured via the relevant web.xml file, in this case: `/m1/voyager/YOURDB/tomcat/vwebv/context/vwebv/WEB-INF/web.xml`.  The build directory has a diff patch, but for more context:

```
The filter is inserted first in the chain (the first `<filter>` element):
<!-- 2014-08-12 akohler: Custom UCLA filter to block aggressive record harvesters.
     IP addresses requesting more than RequestsAllowed records in TrackingInterval
     seconds will be banned (receive HTTP 403) for BanDuration seconds.
-->
<filter>
    <filter-name>BBID Harvest Filter</filter-name>
    <filter-class>edu.ucla.library.libservices.tomcat.filters.BbidHarvestFilter</filter-class>
    <init-param>
        <!-- Number of requests allowed during TrackingInterval -->
        <param-name>RequestsAllowed</param-name>
        <param-value>20</param-value>
    </init-param>
    <init-param>
        <!-- Seconds to ban IP addresses which exceed RequestsAllowed -->
        <param-name>BanDuration</param-name>
        <param-value>60</param-value>
    </init-param>
    <init-param>
        <!-- Amount of time in seconds before now to track previous requests -->
        <param-name>TrackingInterval</param-name>
        <param-value>10</param-value>
    </init-param>
</filter>
```

It has a corresponding `<filter-mapping>` element, also the first in its chain:
```
<!-- 2014-08-12 akohler: Custom UCLA filter to block aggressive record harvesters -->
<filter-mapping>
    <filter-name>BBID Harvest Filter</filter-name>
    <url-pattern>/holdingsInfo/*</url-pattern>
    <url-pattern>/staffView/*</url-pattern>
</filter-mapping>
```

### Enabling / disabling
The filter code gets its parameters from `web.xml`, so code and config must be kept in sync.  If the code or config is changed, Tomcat must be restarted for the changes to take effect.

If the filter must be disabled, comment out the relevant `<filter>` and `<filter-mapping>` elements and restart Tomcat.


