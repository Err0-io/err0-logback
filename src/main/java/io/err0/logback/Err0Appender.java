package io.err0.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Err0Appender extends AppenderBase<ILoggingEvent> {

    public static class Err0Log {
        public Err0Log(final String error_code, final long ts) {
            this.error_code = error_code;
            this.ts = ts;
        }
        public final String error_code;
        public final long ts;
    }

    private static final ConcurrentLinkedQueue<Err0Log> queue = new ConcurrentLinkedQueue<>();

    public Err0Appender() {
        super();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                //System.err.println("shutdown hook");
                stopped = true;
                while (pollQueue()) {}
                Err0Http.shutdown();
            }
        }));
        this.thread.setDaemon(true);
        this.thread.start();
    }

    // properties, and accessors
    private String baseUrl;
    private URL url;
    public String getUrl() { return baseUrl; }
    public void setUrl(String value) { baseUrl = value; try { url = new URL(value + "~/api/bulk-log"); } catch (MalformedURLException e) { url = null; } }

    private String token;
    public String getToken() { return token; }
    public void setToken(String value) { token = value; }

    private static Pattern pattern = Pattern.compile("\\[([A-Z][A-Z0-9]*-[0-9]+)\\]");

    private boolean stopped = false;

    private boolean pollQueue() {
        try {
            ArrayList<Err0Log> list = new ArrayList<>();
            Err0Log logEvent = null;
            do {
                logEvent = queue.poll();
                if (null != logEvent) {
                    list.add(logEvent);
                }
            } while (null != logEvent);
            if (list.size() > 0) {
                JsonObject bulkLog = new JsonObject();
                JsonArray logs = new JsonArray();
                bulkLog.add("logs", logs);
                for (Err0Log log : list) {
                    //System.err.println("ERR0\t" + log.error_code + "\t" + log.ts + "\t" + log.message + "\t" + log.metadata.toString());

                    JsonObject o = new JsonObject();
                    o.addProperty("error_code", log.error_code);
                    o.addProperty("ts", Long.toString(log.ts));

                    logs.add(o);
                }
                Err0Http.call(url, token, bulkLog);
                return true;
            }
        }
        catch (Throwable t) {
            // ignore
        }
        return false;
    }

    private final Thread thread = new Thread() {
        @Override
        public void run() {
            for (;!stopped;) {
                if (!Err0Http.canCall()) {
                    Thread.yield();
                } else {
                    boolean wasEmpty = pollQueue();
                    if (wasEmpty) {
                        Thread.yield();
                    }
                }
            }
        }
    };

    @Override
    protected void append(ILoggingEvent event) {
        final String formattedMessage = event.getFormattedMessage();
        // DEBUG System.out.println("ERR0\t" + formattedMessage);
        final Matcher matcher = pattern.matcher(formattedMessage);
        while (matcher.find()) {
            final String error_code = matcher.group(1);
            final long ts = event.getTimeStamp();
            queue.add(new Err0Log(error_code, ts));
        }
    }
}
