package com.akakata.event;

import com.akakata.communication.MessageBuffer;
import com.akakata.event.impl.SharedNetworkEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;

/**
 * Captures and manages native payload references for asynchronous event deliveries.
 *
 * <p>Payload references are captured independently of mutable events so cleanup
 * always releases the reference that was originally retained.
 *
 * <p>Capture the reference before starting asynchronous work:
 * <pre>{@code
 * Object payload = EventResources.retainPayload(event);
 * try {
 *     // Process the event while its native payload is retained.
 * } finally {
 *     EventResources.releasePayload(payload);
 * }
 * }</pre>
 */
public final class EventResources {

    /**
     * Prevents direct construction of this utility or singleton component.
     */
    private EventResources() {}

    /**
     * Estimates common native, binary, text and decoded JSON payload storage.
     *
     * <p>Traversal is capped at 64 levels; cyclic or exceptionally deep graphs
     * receive the maximum charge and are rejected by ordinary executor budgets.
     * Unknown application objects receive a 256-byte charge; override
     * {@link Event#getPayloadSize()} for larger custom objects.
     *
     * @param source the immutable payload, possibly null
     * @return the estimated storage in bytes
     */
    public static long estimateBytes(Object source) {
        return estimateBytes(source, 0);
    }

    /**
     * Traverses bounded JSON-compatible payload graphs without native copies.
     *
     * @param source the current value
     * @param depth the current traversal depth
     * @return the saturating byte estimate
     */
    private static long estimateBytes(Object source, int depth) {
        if (source == null) {
            return 0;
        }
        if (depth >= 64) {
            return Long.MAX_VALUE;
        }
        io.netty.buffer.ByteBuf java8Pattern8;
        if (((java8Pattern8 = com.akakata.compat.Java8.cast(source, io.netty.buffer.ByteBuf.class)) != null)) {
            return java8Pattern8.readableBytes();
        }
        MessageBuffer<?> java8Pattern9;
        if (((java8Pattern9 = com.akakata.compat.Java8.cast(source, MessageBuffer.class)) != null)) {
            return estimateBytes(java8Pattern9.getNativeBuffer(), depth + 1);
        }
        byte[] java8Pattern10;
        if (((java8Pattern10 = com.akakata.compat.Java8.cast(source, byte[].class)) != null)) {
            return java8Pattern10.length;
        }
        CharSequence java8Pattern11;
        if (((java8Pattern11 = com.akakata.compat.Java8.cast(source, CharSequence.class)) != null)) {
            return 40L + 3L * java8Pattern11.length();
        }
        if (source instanceof Number || source instanceof Boolean) {
            return 24;
        }
        long bytes = 64;
        java.util.Map<?, ?> java8Pattern12;
        if (((java8Pattern12 = com.akakata.compat.Java8.cast(source, java.util.Map.class)) != null)) {
            for (java.util.Map.Entry<?, ?> entry : java8Pattern12.entrySet()) {
                bytes = saturatedAdd(bytes, saturatedAdd(48,
                        saturatedAdd(estimateBytes(entry.getKey(), depth + 1), estimateBytes(entry.getValue(), depth + 1))));
                if (bytes == Long.MAX_VALUE) {
                    return bytes;
                }
            }
            return bytes;
        }
        java.util.Collection<?> java8Pattern13;
        if (((java8Pattern13 = com.akakata.compat.Java8.cast(source, java.util.Collection.class)) != null)) {
            for (Object value : java8Pattern13) {
                bytes = saturatedAdd(bytes, saturatedAdd(8, estimateBytes(value, depth + 1)));
                if (bytes == Long.MAX_VALUE) {
                    return bytes;
                }
            }
            return bytes;
        }
        return 256;
    }

    /**
     * Adds nonnegative charges without wrapping the admission counter.
     *
     * @param left the first charge
     * @param right the second charge
     * @return the sum saturated at the maximum signed long
     */
    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    /**
     * Captures and retains the event's native payload reference.
     *
     * <p>Release the returned reference rather than looking up the event source again,
     * because the event may be mutated after capture.
     *
     * @param event the event to process
     * @return the retained payload, or {@code null} if no native reference exists
     */
    public static Object retainPayload(Event event) {
        return ReferenceCountUtil.retain(payloadReference(event));
    }

    /**
     * Releases one captured native reference. Null and non-reference-counted values are ignored.
     *
     * @param payload the encoded or captured payload
     */
    public static void releasePayload(Object payload) {
        ReferenceCountUtil.release(payload);
    }

    /**
     * Captures the native reference-counted payload without retaining it.
     *
     * @param event the event to process
     * @return the reference-counted payload, or {@code null} for nonnative payloads
     */
    public static Object payloadReference(Event event) {
        SharedNetworkEvent java8Pattern14;
        if (((java8Pattern14 = com.akakata.compat.Java8.cast(event, SharedNetworkEvent.class)) != null)) {
            return java8Pattern14.payloadLease();
        }
        Object source = event.getSource();
        MessageBuffer<?> java8Pattern15;
        Object payload = ((java8Pattern15 = com.akakata.compat.Java8.cast(source, MessageBuffer.class)) != null) ? java8Pattern15.getNativeBuffer() : source;
        return payload instanceof ReferenceCounted ? payload : null;
    }
}
