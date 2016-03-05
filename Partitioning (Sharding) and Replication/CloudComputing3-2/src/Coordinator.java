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
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;

import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
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
    private static final String dataCenter1 = "ec2-52-91-63-222.compute-1.amazonaws.com";
    private static final String dataCenter2 = "ec2-52-87-227-150.compute-1.amazonaws.com";
    private static final String dataCenter3 = "ec2-54-174-158-53.compute-1.amazonaws.com";
    
    // a class define key lock
    public class KeyLock {
        int i = 0;
    }
    
    // map lock
    private ReentrantLock mapLock;
    // a map from key to its queue
    private HashMap<String, PriorityQueue<String>> queueMap;
    // a map from key to lock for the queue
    private HashMap<String, ReentrantLock> queueLocks;
    // a map from key to  lock for itself
    private HashMap<String, KeyLock> keyLockMap;
    // variable for choosing data center
    private static int dcNumber = 0;

    // Hash function
    private String hashMapping(String key) {  
        int n = 0;
        
        if(key.length() == 1) {
            if ( key.charAt(0) == 'a'){
                n = 0;
            }
            else if (key.charAt(0) == 'b') {
                n = 1;
            }else if (key.charAt(0) == 'c'){
                n = 2;
            }else {
                n = n+1;
            }
        }else {
            for(int i = 0; i < key.length(); i++) {
                n += Math.abs(key.charAt(i) - '0');
            }
        }
        n %= 3;
        if(n == 0) {
            return dataCenter1;
        }else if (n == 1) {
            return dataCenter2;
        }else {
            return dataCenter3;
        }       
    }
    
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

        mapLock = new ReentrantLock();
        queueMap = new HashMap<String, PriorityQueue<String>>();
        queueLocks = new HashMap<String, ReentrantLock>();        
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
                mapLock.lock();

                if (!queueMap.containsKey(key)) {
                    queueMap.put(key, new PriorityQueue<String>());
                    queueLocks.put(key, new ReentrantLock());
                    keyLockMap.put(key, new KeyLock());
                }

                queueMap.get(key).add(timestamp);

                // unlock the map
                mapLock.unlock();
                
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        // Each PUT operation is handled in a different thread.                       
                        if(storageType.equals("replication")){
                            replicatePut(key, value, timestamp);
                        } else {
                            shardingPut(key, value, timestamp);
                        }                 
                    }
                    
                    private void replicatePut(String key, String value, String timestamp) {
                        queueLocks.get(key).lock();
                     // wait until the timstamp is equal to the queue head 
                        while ((queueMap.get(key).peek()!=null && !queueMap.get(key).peek().equals(timestamp))) {
                            queueLocks.get(key).unlock();
                            try {
                                synchronized (keyLockMap.get(key)) {
                                    //wait
                                    keyLockMap.get(key).wait();
                                }
                            } catch(Exception e){
                                System.out.println("Exception happens in Replicate Put1");
                            }                    
                            queueLocks.get(key).lock();
                        }
                        queueLocks.get(key).unlock();
                        
                        try{
                            KeyValueLib.PUT(dataCenter1, key, value);
                            KeyValueLib.PUT(dataCenter2, key, value);
                            KeyValueLib.PUT(dataCenter3, key, value);
                        } catch (Exception e){
                            System.out.println("Exception happens in Replicate Put2");
                        }

                        queueLocks.get(key).lock();
                        queueMap.get(key).remove(timestamp);
                        queueLocks.get(key).unlock();
                        
                        synchronized (keyLockMap.get(key)) {
                            //notify the other waiting thread 
                            keyLockMap.get(key).notifyAll();
                        }              
                        return;

                    }
                    
                    private void shardingPut(String key, String value, String timestamp) {
                        queueLocks.get(key).lock();
                        System.out.println("key=" + key);
                        // wait until the timstamp is equal to the queue head  
                        while ((queueMap.get(key).peek()!=null && !queueMap.get(key).peek().equals(timestamp))) {                           
                            queueLocks.get(key).unlock();
                            try {
                                synchronized (keyLockMap.get(key)) {
                                    //wait
                                    keyLockMap.get(key).wait();
                                }
                            } catch(Exception e){
                                System.out.println("Exception happens in Sharding Put1");
                            }                    
                            queueLocks.get(key).lock();
                        }
                        queueLocks.get(key).unlock();
                        
                        String dataCenter = hashMapping(key);
                        try{
                            KeyValueLib.PUT(dataCenter, key, value);
                        } catch (Exception e){
                            System.out.println("Exception happens in Sharding Put2");
                        }

                        queueLocks.get(key).lock();
                        queueMap.get(key).remove(timestamp);
                        queueLocks.get(key).unlock();
                        
                        synchronized (keyLockMap.get(key)) {
                            //notify the other waiting thread 
                            keyLockMap.get(key).notifyAll();
                        }              
                        return;

                    }

                    
                });
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
                
             // lock the map
                mapLock.lock();
                
                if(!queueMap.containsKey(key)) {
                    queueMap.put(key, new PriorityQueue<String>());
                    queueLocks.put(key, new ReentrantLock());
                    keyLockMap.put(key, new KeyLock());
                }
                
                queueMap.get(key).add(timestamp);
                
                //unlock the map
                mapLock.unlock();
                
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        String result;
                        if(storageType.equals("replication")){
                            result = replicateGet(key, timestamp);
                        }else{
                            result = shardingGet(key, loc, timestamp);
                        }
                                        
                        req.response().end(result); // Default response = 0
                    }
                    
                    private String replicateGet(String key, String timestamp) {
                        queueLocks.get(key).lock();
                     // wait until the timstamp is equal to the queue head 
                        while (queueMap.get(key).peek() != null && !queueMap.get(key).peek().equals(timestamp)) {
                            queueLocks.get(key).unlock();
                            try {
                                synchronized (keyLockMap.get(key)) {
                                    //wait
                                    keyLockMap.get(key).wait();                       
                                }
                            }catch (Exception e){
                                
                            }                
                            queueLocks.get(key).lock();
                        }
                        
                        queueLocks.get(key).unlock();
                        String result = "";
                        //distribute the request evenly to 3 data centers
                        dcNumber = (dcNumber + 1) % 3;
                        try{
                            if (dcNumber == 0) {
                                result = KeyValueLib.GET(dataCenter1, key);
                            }else if (dcNumber == 1) {
                                result = KeyValueLib.GET(dataCenter2, key);
                            }else{
                                result = KeyValueLib.GET(dataCenter3, key);
                            }
                                                 
                        } catch (Exception e){
                            System.out.println("Exception happens in Get");
                        }

                        queueLocks.get(key).lock();
                        queueMap.get(key).remove(timestamp);
                        queueLocks.get(key).unlock();
                        
                        synchronized (keyLockMap.get(key)) {
                            //notify the waiting thread of the same key
                            keyLockMap.get(key).notifyAll();
                        }           
                        
                        return result;
                    }                    
                    
                    private String shardingGet(String key, String loc,String timestamp) {
                        System.out.println("key=" + key);
                        queueLocks.get(key).lock();
                        // wait until the timstamp is equal to the queue head 
                        while (queueMap.get(key).peek() != null && !queueMap.get(key).peek().equals(timestamp)) {
                            queueLocks.get(key).unlock();
                            try {
                                synchronized (keyLockMap.get(key)) {
                                    //wait
                                    keyLockMap.get(key).wait();                       
                                }
                            }catch (Exception e){
                                
                            }                
                            queueLocks.get(key).lock();
                        }
                        
                        queueLocks.get(key).unlock();
                        
                        String result = "0";
                        try{
                            if (loc.equals("1") && hashMapping(key).equals(dataCenter1)) {
                                result = KeyValueLib.GET(dataCenter1, key);
                            }else if (loc.equals("2") && hashMapping(key).equals(dataCenter2)) {
                                result = KeyValueLib.GET(dataCenter2, key);
                            } else if (loc.equals("3") && hashMapping(key).equals(dataCenter3)){
                                result = KeyValueLib.GET(dataCenter3, key);
                            }
                        }catch (Exception e) {
                            System.out.println("Exeption happens in Sharding Get");
                        }
                        
                        queueLocks.get(key).lock();
                        queueMap.get(key).remove(timestamp);
                        queueLocks.get(key).unlock();
                        
                        synchronized (keyLockMap.get(key)) {
                            //notify the waiting thread of the same key
                            keyLockMap.get(key).notifyAll();
                        }           
                        
                        return result;
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
