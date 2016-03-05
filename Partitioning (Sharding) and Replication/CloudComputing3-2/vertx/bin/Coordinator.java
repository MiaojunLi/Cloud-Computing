import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.sql.Timestamp;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public class Coordinator extends Verticle {

    // Default mode: sharding. Possible string values are "replication" and
    // "sharding"
    private static String storageType = "replication";

    /**
     * TODO: Set the values of the following variables to the DNS names of your
     * three dataCenter instances
     */
    private static final String dataCenter1 = "<DNS-OF-DATACENTER-1>";
    private static final String dataCenter2 = "<DNS-OF-DATACENTER-2>";
    private static final String dataCenter3 = "<DNS-OF-DATACENTER-3>";

    public class KeyLock {
        int i = 0;
    }

    private ReentrantLock mapPutLock;
    private HashMap<String, PriorityQueue<String>> putMap;
    private HashMap<String, ReentrantLock> queuePutLocks;

    private ReentrantLock mapGetLock;
    private HashMap<String, PriorityQueue<String>> getMap;
    private HashMap<String, ReentrantLock> queueGetLocks;

    private HashMap<String, ReentrantLock> dcLock;
    private HashMap<String, KeyLock> keyLockMap;

    private int dcNumber = 0;

    @Override
    public void start() {
        // DO NOT MODIFY THIS
        KeyValueLib.dataCenters.put(dataCenter1, 1);
        KeyValueLib.dataCenters.put(dataCenter2, 2);
        KeyValueLib.dataCenters.put(dataCenter3, 3);
        final RouteMatcher routeMatcher = new RouteMatcher();
        final HttpServer server = vertx.createHttpServer();
        server.setAcceptBacklog(32767);
        server.setUsePooledBuffers(true);
        server.setReceiveBufferSize(4 * 1024);

        mapPutLock = new ReentrantLock();
        getMap = new HashMap<String, PriorityQueue<String>>();
        queuePutLocks = new HashMap<String, ReentrantLock>();

        mapGetLock = new ReentrantLock();
        getMap = new HashMap<String, PriorityQueue<String>>();
        queueGetLocks = new ReentrantLock();

        dcLock = new HashMap<String, ReentrantLock>();
        keyLockMap = new HashMap<String, KeyLock>();

        routeMatcher.get("/put", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                MultiMap map = req.params();
                final String key = map.get("key");
                final String value = map.get("value");
                // You may use the following timestamp for ordering requests
                final String timestamp = new Timestamp(
                        System.currentTimeMillis() + TimeZone.getTimeZone("EST").getRawOffset()).toString();

                // lock the map
                mapGetLock.lock();
                mapPutLock.lock();

                if (!putMap.containsKey(key)) {
                    getMap.put(key, new PriorityQueue<String>());
                    putMap.put(key, new PriorityQueue<String>());
                    queueGetLocks.put(key, new ReentrantLock());
                    queuePutLocks.put(key, new ReentrantLock());
                    keyLockMap.put(key, new KeyLock());
                }

                putMap.get(key).add(timestamp);

                // unlock the map
                mapGetLock.unlock();
                mapPutLock.unlock();
                
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        // TODO: Write code for PUT operation here.
                        // Each PUT operation is handled in a different thread.
                        // Highly recommended that you make use of helper
                        // functions.
                    }
                    
                    private void replicatePut(String key, String value, String timestamp) {
                        queueGetLocks.get(key).lock();
                        queuePutLocks.get(key).lock();
                        
                        while ((putMap.get(key).peek()!=null && !putMap.get(key).peek().equals(timestamp)) 
                                || (getMap.get(key).peek()!=null && Long.parseLong(getMap.get(key).peek()) > Long.parseLong(timestamp))) {
                            
                            queueGetLocks.get(key).unlock();
                            queuePutLocks.get(key).unlock();
                            try {
                                synchronized (keyLockMap.get(key)) {
                                    keyLockMap.get(key).wait();
                                }
                            } catch(Exception e){
                                
                            }
                           
                            queueGetLocks.get(key).lock();
                            queuePutLocks.get(key).lock();
                        }
                        
                        queueGetLocks.get(key).unlock();
                        queuePutLocks.get(key).unlock();

                        try{
                            KeyValueLib.PUT(dataCenter1, key, value);
                            KeyValueLib.PUT(dataCenter2, key, value);
                            KeyValueLib.PUT(dataCenter3, key, value);
                        } catch (Exception e){
                            System.out.println("Exception happens in Put");
                        }

                        queuePutLocks.get(key).lock();
                        putMap.get(key).remove(timestamp);
                        queuePutLocks.get(key).unlock();
                        
                        keyLockMap.get(key).notifyAll();
                        return;

                    }

                    
                });//runnable
                t.start();
                req.response().end(); // Do not remove this
            }
        });

        routeMatcher.get("/get", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                MultiMap map = req.params();
                final String key = map.get("key");
                final String loc = map.get("loc");
                // You may use the following timestamp for ordering requests
                final String timestamp = new Timestamp(
                        System.currentTimeMillis() + TimeZone.getTimeZone("EST").getRawOffset()).toString();
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        // TODO: Write code for GET operation here.
                        // Each GET operation is handled in a different thread.
                        // Highly recommended that you make use of helper
                        // functions.
                        req.response().end("0"); // Default response = 0
                    }
                });
                t.start();
            }
        });

        routeMatcher.get("/storage", new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                MultiMap map = req.params();
                storageType = map.get("storage");
                // This endpoint will be used by the auto-grader to set the
                // consistency type that your key-value store has to support.
                // You can initialize/re-initialize the required data structures
                // here
                req.response().end();
            }
        });

        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest req) {
                req.response().putHeader("Content-Type", "text/html");
                String response = "Not found.";
                req.response().putHeader("Content-Length", String.valueOf(response.length()));
                req.response().end(response);
                req.response().close();
            }
        });
        server.requestHandler(routeMatcher);
        server.listen(8080);
    }
}
