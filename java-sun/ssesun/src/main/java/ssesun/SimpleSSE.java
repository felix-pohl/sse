package ssesun;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SimpleSSE {

    public static List<HttpExchange> connections = new CopyOnWriteArrayList<>();
    public static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(10);

    public static void main(String[] args) throws Exception {
        registerGlobalExceptionHandler();
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        try {
            server.createContext("/test", new SSEHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Runnable heartbeatCommand = new HeartbeatRunnable();
        scheduledThreadPoolExecutor.scheduleAtFixedRate(heartbeatCommand, 0, 1, TimeUnit.SECONDS);
        server.setExecutor(
            //new ThreadPoolExecutor(20,20,100,TimeUnit.SECONDS, queue)
            new ForkJoinPool(1024)
            );
        server.start();
    }



    private static void registerGlobalExceptionHandler() {
        ExHandler globalExceptionHandler = new ExHandler();
        Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
    }

    
    static class ExHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            System.out.println("Unhandled exception caught!");
        }
    }

    static class HeartbeatRunnable implements Runnable {
        @Override
        public void run() {
            try {
                heartbeat();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void heartbeat() throws IOException {
            Iterator<HttpExchange> iterator = connections.iterator();
            while(iterator.hasNext()) {
                HttpExchange next = iterator.next();
                try {
                    OutputStream responseBody = next.getResponseBody();
                    responseBody.write(":hb\n\n".getBytes());
                    responseBody.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    connections.remove(next);
                    sendConnections();
                }
            }
        }
    }

    static private void sendConnections() throws IOException {
        Iterator<HttpExchange> iterator = connections.iterator();
        while(iterator.hasNext()) {
            HttpExchange next = iterator.next();
            try {
                OutputStream responseBody = next.getResponseBody();
                String response = "event:connections\ndata:" + connections.size() +"\n\n";
                responseBody.write(response.getBytes());
                responseBody.flush();
            } catch (Exception e) {
                e.printStackTrace();
                connections.remove(next);
                sendConnections();
            }
        }
    }

    static class SSEHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.getResponseHeaders().add("Content-Type", "text/event-stream");
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.sendResponseHeaders(200, 0);
            connections.add(t);
            sendConnections();
        }
    }
}
