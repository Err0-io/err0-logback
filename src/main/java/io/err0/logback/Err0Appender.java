package io.err0.logback;

import ch.qos.logback.core.AppenderBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Err0Appender extends AppenderBase {

    public static class Err0Log {
        public Err0Log(final String error_code, final long ts, final String message, final JsonObject metadata) {
            this.error_code = error_code;
            this.ts = ts;
            this.message = message;
            this.metadata = metadata;
        }
        public final String error_code;
        public final long ts;
        public final String message;
        public final JsonObject metadata;
    }



    private static final ConcurrentLinkedQueue<Err0Log> queue = new ConcurrentLinkedQueue<>();

    protected Err0Appender(String url, String token, String realm_uuid, String prj_uuid, String pattern) {
        super();
        this.baseUrl = url;
        try {
            this.url = new URL(url + "~/api/bulk-log");
        }
        catch (MalformedURLException e) {
            System.err.println(e.getMessage());
            this.url = null;
        }
        this.token = token;
        this.realm_uuid = realm_uuid;
        this.prj_uuid = prj_uuid;
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
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

    private final String baseUrl;
    private URL url;
    private final String token;
    private final String realm_uuid;
    private final String prj_uuid;
    private final Pattern pattern;
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
                bulkLog.addProperty("realm_uuid", realm_uuid);
                bulkLog.addProperty("prj_uuid", prj_uuid);
                JsonArray logs = new JsonArray();
                bulkLog.add("logs", logs);
                for (Err0Log log : list) {
                    //System.err.println("ERR0\t" + log.error_code + "\t" + log.ts + "\t" + log.message + "\t" + log.metadata.toString());

                    JsonObject o = new JsonObject();
                    o.addProperty("error_code", log.error_code);
                    o.addProperty("ts", Long.toString(log.ts));
                    o.addProperty("msg", log.message);
                    o.add("metadata", log.metadata);

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
    protected void append(Object eventObject) {
        final String formattedMessage = event.getMessage().getFormattedMessage();
        final Matcher matcher = pattern.matcher(formattedMessage);
        if (matcher.find()) {
            final String error_code = matcher.group(1);
            final long ts = event.getTimeMillis();
            final JsonObject metadata = new JsonObject();
            final JsonObject log4j2Metadata = new JsonObject();
            final Level level = event.getLevel();
            final StackTraceElement source = event.getSource();
            log4j2Metadata.addProperty("level", level.name());
            log4j2Metadata.addProperty("source_class", source.getClassName());
            log4j2Metadata.addProperty("source_file", source.getFileName());
            log4j2Metadata.addProperty("source_line", source.getLineNumber());
            metadata.add("log4j2", log4j2Metadata);
            queue.add(new Err0Log(error_code, ts, formattedMessage, metadata));
        }
    }
}
