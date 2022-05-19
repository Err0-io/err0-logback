package io.err0.logback;

import com.google.gson.JsonObject;
import org.apache.hc.client5.http.async.methods.*;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.function.Factory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.ssl.TlsDetails;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Err0Http {
    final static CloseableHttpAsyncClient client;
    static {
        final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                .useSystemProperties()
                // IMPORTANT uncomment the following method when running Java 9 or older
                // in order for ALPN support to work and avoid the illegal reflective
                // access operation warning
                .setTlsDetailsFactory(new Factory<SSLEngine, TlsDetails>() {
                    @Override
                    public TlsDetails create(final SSLEngine sslEngine) {
                        return new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol());
                    }
                })
                .build();
        final PoolingAsyncClientConnectionManager cm = PoolingAsyncClientConnectionManagerBuilder.create()
                .setTlsStrategy(tlsStrategy)
                .build();
        client = HttpAsyncClients.custom()
                .setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
                .setConnectionManager(cm)
                .build();

        client.start();
    }
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

        inFlight.incrementAndGet();
        try {
            final HttpHost target = new HttpHost(url.getProtocol(), url.getHost(), url.getPort());
            final HttpClientContext clientContext = HttpClientContext.create();
            final SimpleHttpRequest request = SimpleRequestBuilder.post()
                    .setHttpHost(target)
                    .setPath(url.getPath())
                    .setHeader("Authorization", "Bearer " + token)
                    .setBody(payload.toString(), ContentType.APPLICATION_JSON)
                    .build();

            final Future<SimpleHttpResponse> future = client.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    clientContext,
                    new FutureCallback<SimpleHttpResponse>() {
                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            inFlight.decrementAndGet();
                            errorUntil.set(0);
                        }

                        @Override
                        public void failed(final Exception ex) {
                            System.err.println("ERR0R!!!" + (null == ex ? "ERR0 http failure" : ex.getMessage()));
                            inFlight.decrementAndGet();
                            errorUntil.set(new Date().getTime() + (30L*60L*1000L)); // 30 minutes before a retry
                        }

                        @Override
                        public void cancelled() {
                            inFlight.decrementAndGet();
                        }
                    });
        }
        catch (Exception ex) {
            // ignore
        }
    }

    public static void shutdown() {
        //System.out.println("Shutting down");
        while (inFlight.get() > 0) {
            Thread.yield();
        }
        client.close(CloseMode.GRACEFUL);
    }
}
