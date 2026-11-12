package com.akakata.server;

/**
 * Bounds HTTP aggregation and absolute request or read-inactivity deadlines.
 *
 */
public final class HttpLimits {
    /**
     * Immutable value of maximumRequestBytes.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumRequestBytes")
    private final int maximumRequestBytes;

    /**
     * Immutable value of requestTimeoutMillis.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("requestTimeoutMillis")
    private final int requestTimeoutMillis;

    /**
     * Immutable value of readIdleMillis.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("readIdleMillis")
    private final int readIdleMillis;

    /**
     * Immutable value of maximumConnections.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumConnections")
    private final int maximumConnections;


    /**
     * Default policy: 256 sockets, 64 KiB bodies, 10-second requests and 30-second read inactivity.
     */
    public static final HttpLimits DEFAULT = new HttpLimits(65_536, 10_000, 30_000, 256);

    /**
     * Creates a request policy with a default allowance of 256 HTTP connections.
     *
     * @param maximumRequestBytes the positive body allowance
     * @param requestTimeoutMillis the positive request deadline
     * @param readIdleMillis the nonnegative read inactivity deadline
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public HttpLimits(@com.fasterxml.jackson.annotation.JsonProperty("maximumRequestBytes") int maximumRequestBytes, @com.fasterxml.jackson.annotation.JsonProperty("requestTimeoutMillis") int requestTimeoutMillis, @com.fasterxml.jackson.annotation.JsonProperty("readIdleMillis") int readIdleMillis) {
        this(maximumRequestBytes, requestTimeoutMillis, readIdleMillis, 256);
    }

    /**
     * Validates the HTTP policy before a listener is constructed.
     *
     * @param maximumRequestBytes the positive body allowance
     * @param requestTimeoutMillis the positive request deadline
     * @param readIdleMillis the nonnegative read inactivity deadline
     * @param maximumConnections the positive HTTP socket allowance
     * @throws IllegalArgumentException if a value is outside its supported range
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public HttpLimits(@com.fasterxml.jackson.annotation.JsonProperty("maximumRequestBytes") int maximumRequestBytes, @com.fasterxml.jackson.annotation.JsonProperty("requestTimeoutMillis") int requestTimeoutMillis, @com.fasterxml.jackson.annotation.JsonProperty("readIdleMillis") int readIdleMillis, @com.fasterxml.jackson.annotation.JsonProperty("maximumConnections") int maximumConnections) {
        if (maximumRequestBytes <= 0 || requestTimeoutMillis <= 0 || readIdleMillis < 0 || maximumConnections <= 0) {
            throw new IllegalArgumentException("Invalid HTTP limits");
        }

        this.maximumRequestBytes = maximumRequestBytes;
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.readIdleMillis = readIdleMillis;
        this.maximumConnections = maximumConnections;
    }

    /**
     * Returns the immutable maximumRequestBytes value.
     *
     * @return maximumRequestBytes value
     */
    public int maximumRequestBytes() {
        return maximumRequestBytes;
    }

    /**
     * Returns the immutable requestTimeoutMillis value.
     *
     * @return requestTimeoutMillis value
     */
    public int requestTimeoutMillis() {
        return requestTimeoutMillis;
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
     * Returns the immutable maximumConnections value.
     *
     * @return maximumConnections value
     */
    public int maximumConnections() {
        return maximumConnections;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof HttpLimits)) {
            return false;
        }
        HttpLimits other = (HttpLimits) value;
        return maximumRequestBytes == other.maximumRequestBytes
                && requestTimeoutMillis == other.requestTimeoutMillis
                && readIdleMillis == other.readIdleMillis
                && maximumConnections == other.maximumConnections;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.lang.Integer.hashCode(maximumRequestBytes);
        result = 31 * result + java.lang.Integer.hashCode(requestTimeoutMillis);
        result = 31 * result + java.lang.Integer.hashCode(readIdleMillis);
        result = 31 * result + java.lang.Integer.hashCode(maximumConnections);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "HttpLimits[maximumRequestBytes=" + maximumRequestBytes
                + ", requestTimeoutMillis=" + requestTimeoutMillis
                + ", readIdleMillis=" + readIdleMillis
                + ", maximumConnections=" + maximumConnections
                + "]";
    }
}
