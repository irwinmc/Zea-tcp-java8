package com.akakata.handlers.codec;

import com.akakata.context.ConfigurationManager;
import com.akakata.event.Event;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;
import java.util.Objects;

/**
 * Encodes events as bounded JSON application frames.
 *
 * <p>Shared Game sends reuse a canonical read-only application frame with independent
 * recipient indices. Ordinary sends serialize directly to bytes without String conversion.
 */
@Sharable
public class WebSocketEventEncoder extends MessageToMessageEncoder<Event> {

    /**
     * WebSocket-only limit on application bytes, excluding the WebSocket header.
     */
    private static final String LIMIT_PROPERTY = "web.socket.maximum.outbound.frame.bytes";

    /**
     * Maximum application bytes per frame, including the opcode and optional length.
     */
    private final int maximumFrameBytes;

    /**
     * Serializer for this encoder's payload format.
     */
    private final JsonPayloadWriter writer;

    /**
     * Creates an encoder with the historical 65,535-byte limit.
     */
    public WebSocketEventEncoder() {
        maximumFrameBytes = EventFrames.TCP_MAXIMUM_FRAME_BYTES;
        writer = EventFrames.JSON_WRITER;
    }

    /**
     * Creates a configured encoder using the default JSON format.
     *
     * @param configuration the framework properties
     */
    public WebSocketEventEncoder(ConfigurationManager configuration) {
        this(configuration, EventFrames.JSON_WRITER);
    }

    /**
     * Creates a configured encoder with an application-specific JSON format.
     *
     * @param configuration the framework properties
     * @param writer thread-safe UTF-8 JSON serializer
     * @throws IllegalArgumentException if the configured limit is malformed or nonpositive
     */
    public WebSocketEventEncoder(ConfigurationManager configuration, JsonPayloadWriter writer) {
        maximumFrameBytes = configuration.getInt(LIMIT_PROPERTY, EventFrames.TCP_MAXIMUM_FRAME_BYTES);
        if (maximumFrameBytes < 1) {
            throw new IllegalArgumentException(LIMIT_PROPERTY + " must be positive");
        }
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Event event, List<Object> out) throws Exception {
        out.add(new BinaryWebSocketFrame(EventFrames.json(ctx.alloc(), event, maximumFrameBytes, writer)));
    }
}
