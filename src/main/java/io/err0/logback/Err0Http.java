package io.err0.logback;

import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Err0Http {
    final static Logger logger = LoggerFactory.getLogger(Err0Http.class);
    final static AtomicInteger inFlight = new AtomicInteger(0);
    final static AtomicLong errorUntil = new AtomicLong(0L);
    public static boolean canCall() {
        return inFlight.get() < 4;
    }
    public static void call(final URL url, final String token, final JsonObject payload)
    {
        long l = errorUntil.get();
        if (l != 0) {
            if (new Date().getTime() < l) {
                return; // silently drop logs, there is an error on the http.
            }
        }

        final byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);

        inFlight.incrementAndGet();

        final OkHttpClient client = new OkHttpClient.Builder()
                .build();

        Request request = new Request.Builder()
                .url(url.toExternalForm())
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Length", Long.toString(body.length))
                .addHeader("Content-Type", "applicaton/json; charset=utf-8")
                .post(RequestBody.create(body))
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            public void onResponse(Call call, Response response)
                    throws IOException {
                inFlight.decrementAndGet();
            }

            public void onFailure(Call call, IOException e) {
                // can't log error
                logger.warn("Failed: " + url.toExternalForm(), e);
                inFlight.decrementAndGet();
            }
        });
    }

    public static void shutdown() {
        //System.out.println("Shutting down");
        while (inFlight.get() > 0) {
            Thread.yield();
        }
    }
}
