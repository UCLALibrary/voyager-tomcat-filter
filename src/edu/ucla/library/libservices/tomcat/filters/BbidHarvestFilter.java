package edu.ucla.library.libservices.tomcat.filters;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class BbidHarvestFilter implements Filter {
  // Map with IP address as key, queue of datetimes requested as value
  static Map<String, ArrayDeque<Long>> ipAddresses = new HashMap<String, ArrayDeque<Long>>();

  // Map with IP address and number of times banned
  static Map<String, Integer> bannedIpAddresses = new HashMap<String, Integer>();

  // Parameters are in seconds, Java times are in milliseconds
  static final long SECONDS_TO_MILLIS = 1000;

  ///// Set in init() via parameters in web.xml
  static int ALLOWED;	// Number of requests allowed in INTERVAL before banning
  static int DURATION;	// Number of seconds to block banned addresses
  static int INTERVAL;	// Number of seconds of tracked activity
  ///// Set in init() via parameters in web.xml

  public void init(FilterConfig filterConfig) throws ServletException {
    this.ALLOWED = Integer.parseInt(filterConfig.getInitParameter("RequestsAllowed"));
    this.DURATION = Integer.parseInt(filterConfig.getInitParameter("BanDuration"));
    this.INTERVAL = Integer.parseInt(filterConfig.getInitParameter("TrackingInterval"));
  }

  // No implementation needed unless resources must be freed
  public void destroy() {}

  // All the action is in doFilter()
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    
    HttpServletResponse httpResponse = (HttpServletResponse) response; 
    String ipAddress = request.getRemoteAddr();
    long requestTime = System.currentTimeMillis();

    // Clean up old entries before evaluating current request
    removeOldEntries(ipAddress, requestTime);

    if (ipAddressNeedsBanning(ipAddress, requestTime)) {
      banIpAddress(ipAddress, requestTime);

      // Testing
      // TODO: Enable this via init-param in web.xml?
      int count = bannedIpAddresses.get(ipAddress);
      long bannedUntil = getLatestRequestTime(ipAddress);
      httpResponse.addHeader("X-Banned-IP", ipAddress + "***" + count + "***" + new java.util.Date(bannedUntil));
      // Testing

      // Return HTTP 403, with Ex Libris default page display
      httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    else {
      // Add an entry for the current request
      addEntry(ipAddress, requestTime);
      // Let WebVoyage handle the request normally
      chain.doFilter(request, response);
    }
  }

  private void removeOldEntries(String ipAddress, long requestTime) {
    // Remove entries for this ipAddress which are older than (requestTime - interval).
    // There's no general cleanup routine, just IP-specific.
    if (ipAddresses.containsKey(ipAddress)) {
      long expireTime = requestTime - (INTERVAL * SECONDS_TO_MILLIS);
      ArrayDeque<Long> times = ipAddresses.get(ipAddress);
      Iterator<Long> iter = times.iterator();
      while (iter.hasNext()) {
	long time = iter.next();
	if (time <= expireTime) {
	  iter.remove();
	}
	// Times *should* all be in chrono order now - if so, could break to exit
      }
      // If all times removed, delete the map entry altogether
      if (times.size() == 0) {
	ipAddresses.remove(ipAddress);
      }
    }
  }

  private boolean ipAddressNeedsBanning(String ipAddress, long requestTime) {
    // Called after removing old entries, before adding new one.
    // If ALLOWED threshold has been reached, we will ban this IP address temporarily.
    // Also (extend the) ban if already banned (latest request already has a penalty,
    // so is later than current request time).
    int requests = 0;
    if (ipAddresses.containsKey(ipAddress)) {
      requests = ipAddresses.get(ipAddress).size();
    }
    long latestRequestTime = getLatestRequestTime(ipAddress);
    // Ban if either condition is true
    return ( (requests >= ALLOWED) || (latestRequestTime > requestTime) );
  }

  private void banIpAddress(String ipAddress, long requestTime) {
    // Add penalty of DURATION seconds to this request,
    // multiplied by number of times banned
    flagIpAsBanned(ipAddress);
    int banFactor = getNumberOfTimesBanned(ipAddress);
    requestTime += (banFactor * DURATION * SECONDS_TO_MILLIS);
    addEntry(ipAddress, requestTime);
  }

  private void addEntry(String ipAddress, long requestTime) {
    ArrayDeque<Long> times;
    if (ipAddresses.containsKey(ipAddress)) {
      times = ipAddresses.get(ipAddress);
    }
    else {
      times = new ArrayDeque<Long>();
    }
    times.addLast(requestTime);
    ipAddresses.put(ipAddress, times);
  }

  private void flagIpAsBanned(String ipAddress) {
    int timesBanned = getNumberOfTimesBanned(ipAddress);
    timesBanned += 1;
    bannedIpAddresses.put(ipAddress, timesBanned);
  }

  private long getLatestRequestTime(String ipAddress) {
    long latestRequestTime = 0; 
    if (ipAddresses.containsKey(ipAddress)) {
      ArrayDeque<Long> times = ipAddresses.get(ipAddress);
      // Times should be in chrono order in queue
      latestRequestTime = times.getLast();
    }
    return latestRequestTime;
  }

  private int getNumberOfTimesBanned(String ipAddress) {
    int timesBanned = 0;
    if (bannedIpAddresses.containsKey(ipAddress)) {
      timesBanned = bannedIpAddresses.get(ipAddress);
    }
    return timesBanned;
  }

}

