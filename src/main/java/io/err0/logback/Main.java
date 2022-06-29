package io.err0.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exists only to test the software against a local instance
 * of err0server.
 */
public class Main {

    static
    {
        // for test, load configuration from the file in the project directory
        System.setProperty("logback.configurationFile", "logback.xml");
    }

    public static void main(String[] args) {
        try {
            Logger logger = LoggerFactory.getLogger(Main.class);
            logger.debug("[EG-2] debug");
            logger.info("[EG-3] info");
            logger.warn("[EG-4] warn");
            logger.error("[EG-5] error");
        }
        catch (Throwable t) {
            System.err.println(t.getMessage());
        }
    }
}
