package com.akakata.handlers.codec;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes the JSON payload for applications with an existing serialization contract.
 * Instances are shared across channels and identify the format for fan-out caching.
 */
@FunctionalInterface
public interface JsonPayloadWriter {

    /**
     * Writes one complete UTF-8 JSON value to the borrowed stream.
     *
     * @param output the bounded destination after the application header
     * @param source the non-null event payload
     * @throws IOException if serialization fails
     */
    void write(OutputStream output, Object source) throws IOException;
}
