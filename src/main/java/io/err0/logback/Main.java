package io.err0.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

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
