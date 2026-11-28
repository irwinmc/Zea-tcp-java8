package com.akakata.handlers.codec;

import com.akakata.event.Event;
import com.akakata.event.impl.SharedNetworkEvent;
import com.akakata.metrics.FlowControlMetrics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.TooLongFrameException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Builds bounded application frames without UTF-8 String round trips or composite allocations.
 * Shared Game sends cache one frame per format while preserving recipient reader indices.
 */
public final class EventFrames {

    /**
     * Thread-safe JSON mapper shared by TCP, WebSocket and binary fallback encodings.
     */
    private static final ObjectMapper MAPPER = com.akakata.compat.Json8.builder().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).build();

    /**
     * Largest application frame representable by the two-byte transport prefix.
     */
    static final int TCP_MAXIMUM_FRAME_BYTES = 65_535;

    /**
     * Default wire format shared by the TCP and WebSocket JSON encoders.
     */
    static final JsonPayloadWriter JSON_WRITER = MAPPER::writeValue;

    /**
     * Prevents construction of this encoding utility.
     */
    private EventFrames() {
    }

    /**
     * Encodes an opcode with an optional length-prefixed JSON object.
     *
     * @param allocator the recipient's pooled allocator
     * @param event immutable event metadata and source
     * @return caller-owned application frame excluding transport headers
     */
    public static ByteBuf json(ByteBufAllocator allocator, Event event) {
        return json(allocator, event, TCP_MAXIMUM_FRAME_BYTES, JSON_WRITER);
    }

    /**
     * Returns an owned frame within the recipient's limit, including on a cache hit.
     *
     * @param allocator the recipient's allocator
     * @param event the outbound event
     * @param maximumFrameBytes the complete application frame limit
     * @param writer the JSON wire format used by this recipient
     * @return caller-owned application frame
     */
    static ByteBuf json(ByteBufAllocator allocator, Event event, int maximumFrameBytes, JsonPayloadWriter writer) {
        if (event instanceof SharedNetworkEvent) {
            SharedNetworkEvent shared = (SharedNetworkEvent) event;
            Object format = writer == JSON_WRITER ? "JSON" : writer;
            ByteBuf frame = shared.encodedFrame(format, () -> encodeJson(allocator, event, maximumFrameBytes, writer));
            if (frame.readableBytes() > maximumFrameBytes) {
                frame.release();
                throw new TooLongFrameException("Application frame exceeds " + maximumFrameBytes + " bytes");
            }
            return frame;
        }
        return encodeJson(allocator, event, maximumFrameBytes, writer);
    }

    /**
     * Streams JSON into one bounded native frame and releases partial output on failure.
     *
     * @param allocator the native allocator
     * @param event the source event
     * @param maximumFrameBytes the complete application frame limit
     * @param writer the serializer for this wire format
     * @return newly allocated application frame
     */
    private static ByteBuf encodeJson(ByteBufAllocator allocator, Event event, int maximumFrameBytes,
                                      JsonPayloadWriter writer) {
        Object source = event.getSource();
        ByteBuf frame = allocator.buffer(source == null ? 1 : Math.min(256, maximumFrameBytes), maximumFrameBytes);
        FrameOutputStream output = null;
        try {
            frame.writeByte(event.getType());
            if (source != null) {
                output = new FrameOutputStream(frame);
                output.checkRemaining(Integer.BYTES);
                frame.writeInt(0);
                try {
                    writer.write(output, source);
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                }
                if (output.overflow != null) {
                    throw output.overflow;
                }
                frame.setInt(1, frame.writerIndex() - 5);
            }
            FlowControlMetrics.ENCODED_FRAMES.increment();
            return frame;
        } catch (RuntimeException | Error failure) {
            frame.release();
            if (output != null && output.overflow != null) {
                throw output.overflow;
            }
            throw failure;
        }
    }

    /**
     * Encodes the project's eight-byte little-endian binary header and payload.
     *
     * @param allocator the recipient's pooled allocator
     * @param event immutable event metadata and source
     * @return caller-owned SBE-style frame excluding the TCP length prefix
     */
    public static ByteBuf sbe(ByteBufAllocator allocator, Event event) {
        SharedNetworkEvent java8Pattern30;
        if (((java8Pattern30 = com.akakata.compat.Java8.cast(event, SharedNetworkEvent.class)) != null)) {
            return java8Pattern30.encodedFrame("SBE", () -> encodeSbe(allocator, event));
        }
        return encodeSbe(allocator, event);
    }

    /**
     * Builds one canonical binary frame without advancing a native source reader index.
     *
     * @param allocator the native allocator
     * @param event the source event
     * @return newly allocated application frame
     */
    private static ByteBuf encodeSbe(ByteBufAllocator allocator, Event event) {
        Object source = event.getSource();
        ByteBuf java8Pattern31;
        ByteBuf nativeSource = ((java8Pattern31 = com.akakata.compat.Java8.cast(source, ByteBuf.class)) != null) ? java8Pattern31 : null;
        byte[] java8Pattern32;
        CharSequence java8Pattern33;
        byte[] payload = nativeSource != null || source == null ? null
                : ((java8Pattern32 = com.akakata.compat.Java8.cast(source, byte[].class)) != null) ? java8Pattern32
                : ((java8Pattern33 = com.akakata.compat.Java8.cast(source, CharSequence.class)) != null) ? java8Pattern33.toString().getBytes(StandardCharsets.UTF_8)
                : com.akakata.compat.Json8.writeValueAsBytes(MAPPER, source);
        int length = nativeSource != null ? nativeSource.readableBytes() : payload == null ? 0 : payload.length;
        validateSize(8 + length);
        if (event.getType() < 0 || event.getType() > 255) {
            throw new IllegalArgumentException("Invalid event opcode");
        }
        ByteBuf frame = allocator.buffer(8 + length, 8 + length);
        frame.writeShortLE(length).writeShortLE(event.getType()).writeShortLE(1).writeShortLE(1);
        if (nativeSource != null) {
            frame.writeBytes(nativeSource, nativeSource.readerIndex(), length);
        } else if (payload != null) {
            frame.writeBytes(payload);
        }
        FlowControlMetrics.ENCODED_FRAMES.increment();
        return frame;
    }

    /**
     * Rejects frames that cannot be represented by the unsigned two-byte TCP prefix.
     *
     * @param bytes the total application frame length
     * @throws TooLongFrameException if the frame exceeds 65535 bytes
     */
    private static void validateSize(int bytes) {
        if (bytes < 0 || bytes > TCP_MAXIMUM_FRAME_BYTES) {
            throw new TooLongFrameException("Application frame exceeds 65535 bytes");
        }
    }

    /**
     * Writes JSON without a whole-payload heap array or transferring native ownership.
     * Closing this stream does not release the caller-owned frame.
     */
    private static final class FrameOutputStream extends OutputStream {

        /**
         * Frame owned by the surrounding encoder until successful publication.
         */
        private final ByteBuf frame;

        /**
         * Captured limit failure preserved when Jackson wraps an output exception.
         */
        private TooLongFrameException overflow;

        /**
         * Creates a stream that borrows one partially written application frame.
         *
         * @param frame the frame receiving the serialized payload
         */
        private FrameOutputStream(ByteBuf frame) {
            this.frame = frame;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(int value) {
            checkRemaining(1);
            frame.writeByte(value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(byte[] bytes, int offset, int length) {
            com.akakata.compat.Java8.checkFromIndexSize(offset, length, bytes.length);
            checkRemaining(length);
            frame.writeBytes(bytes, offset, length);
        }

        /**
         * Rejects a write before native growth can exceed the protocol frame budget.
         *
         * @param length the nonnegative number of bytes to append
         * @throws TooLongFrameException if the complete frame would exceed its configured limit
         */
        private void checkRemaining(int length) {
            if (length > frame.maxCapacity() - frame.writerIndex()) {
                overflow = new TooLongFrameException("Application frame exceeds " + frame.maxCapacity() + " bytes");
                throw overflow;
            }
        }
    }
}
