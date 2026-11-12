package com.akakata.server;

/**
 * Defines admission shared by all listeners and TCP/WebSocket input lifetime limits.
 *
 */
public final class TransportLimits {
    /**
     * Immutable value of maximumConnections.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumConnections")
    private final int maximumConnections;

    /**
     * Immutable value of loginTimeoutMillis.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("loginTimeoutMillis")
    private final int loginTimeoutMillis;

    /**
     * Immutable value of readIdleMillis.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("readIdleMillis")
    private final int readIdleMillis;

    /**
     * Immutable value of maximumFrameBytes.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumFrameBytes")
    private final int maximumFrameBytes;


    /**
     * Default connection policy: 20000 sockets, 30-second login and 120-second read idle.
     */
    public static final TransportLimits DEFAULT = new TransportLimits(20_000, 30_000, 120_000, 65_535);

    /**
     * Validates all limits before opening a listener.
     *
     * @param maximumConnections maximum admitted socket connections
     * @param loginTimeoutMillis login deadline in milliseconds
     * @param readIdleMillis read-idle deadline in milliseconds
     * @param maximumFrameBytes maximum representable application frame bytes
     * @throws IllegalArgumentException if a limit is outside its documented range
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public TransportLimits(@com.fasterxml.jackson.annotation.JsonProperty("maximumConnections") int maximumConnections, @com.fasterxml.jackson.annotation.JsonProperty("loginTimeoutMillis") int loginTimeoutMillis, @com.fasterxml.jackson.annotation.JsonProperty("readIdleMillis") int readIdleMillis, @com.fasterxml.jackson.annotation.JsonProperty("maximumFrameBytes") int maximumFrameBytes) {
        if (maximumConnections <= 0 || loginTimeoutMillis <= 0 || readIdleMillis < 0
                || maximumFrameBytes < 8 || maximumFrameBytes > 65_535) {
            throw new IllegalArgumentException("Invalid transport limits");
        }

        this.maximumConnections = maximumConnections;
        this.loginTimeoutMillis = loginTimeoutMillis;
        this.readIdleMillis = readIdleMillis;
        this.maximumFrameBytes = maximumFrameBytes;
    }

    /**
     * Returns the immutable maximumConnections value.
     *
     * @return maximumConnections value
     */
    public int maximumConnections() {
        return maximumConnections;
    }

    /**
     * Returns the immutable loginTimeoutMillis value.
     *
     * @return loginTimeoutMillis value
     */
    public int loginTimeoutMillis() {
        return loginTimeoutMillis;
    }

    /**
     * Returns the immutable readIdleMillis value.
     *
     * @return readIdleMillis value
     */
    public int readIdleMillis() {
        return readIdleMillis;
    }

    /**
     * Returns the immutable maximumFrameBytes value.
     *
     * @return maximumFrameBytes value
     */
    public int maximumFrameBytes() {
        return maximumFrameBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof TransportLimits)) {
            return false;
        }
        TransportLimits other = (TransportLimits) value;
        return maximumConnections == other.maximumConnections
                && loginTimeoutMillis == other.loginTimeoutMillis
                && readIdleMillis == other.readIdleMillis
                && maximumFrameBytes == other.maximumFrameBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.lang.Integer.hashCode(maximumConnections);
        result = 31 * result + java.lang.Integer.hashCode(loginTimeoutMillis);
        result = 31 * result + java.lang.Integer.hashCode(readIdleMillis);
        result = 31 * result + java.lang.Integer.hashCode(maximumFrameBytes);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "TransportLimits[maximumConnections=" + maximumConnections
                + ", loginTimeoutMillis=" + loginTimeoutMillis
                + ", readIdleMillis=" + readIdleMillis
                + ", maximumFrameBytes=" + maximumFrameBytes
                + "]";
    }
}
