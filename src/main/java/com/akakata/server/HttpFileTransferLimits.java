package com.akakata.server;

/**
 * Bounds static-file handles independently of encoded socket buffer allowances.
 *
 * @since 0.8.5
 */
public final class HttpFileTransferLimits {
    /**
     * Immutable value of maximumPendingPerConnection.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumPendingPerConnection")
    private final int maximumPendingPerConnection;

    /**
     * Immutable value of maximumConcurrentTransfers.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumConcurrentTransfers")
    private final int maximumConcurrentTransfers;

    /**
     * Immutable value of timeoutMillis.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("timeoutMillis")
    private final long timeoutMillis;


    /**
     * Default policy: two transfers per connection, 128 total and a 30-second deadline.
     */
    public static final HttpFileTransferLimits DEFAULT = new HttpFileTransferLimits(2, 128, 30_000);

    /**
     * Validates file-transfer admission before constructing a listener.
     *
     * @param maximumPendingPerConnection positive per-connection allowance
     * @param maximumConcurrentTransfers positive aggregate allowance
     * @param timeoutMillis positive transfer deadline
     * @throws IllegalArgumentException if a limit is not positive
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public HttpFileTransferLimits(@com.fasterxml.jackson.annotation.JsonProperty("maximumPendingPerConnection") int maximumPendingPerConnection, @com.fasterxml.jackson.annotation.JsonProperty("maximumConcurrentTransfers") int maximumConcurrentTransfers, @com.fasterxml.jackson.annotation.JsonProperty("timeoutMillis") long timeoutMillis) {
        if (maximumPendingPerConnection <= 0 || maximumConcurrentTransfers <= 0 || timeoutMillis <= 0) {
            throw new IllegalArgumentException("HTTP file-transfer limits must be positive");
        }

        this.maximumPendingPerConnection = maximumPendingPerConnection;
        this.maximumConcurrentTransfers = maximumConcurrentTransfers;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns the immutable maximumPendingPerConnection value.
     *
     * @return maximumPendingPerConnection value
     */
    public int maximumPendingPerConnection() {
        return maximumPendingPerConnection;
    }

    /**
     * Returns the immutable maximumConcurrentTransfers value.
     *
     * @return maximumConcurrentTransfers value
     */
    public int maximumConcurrentTransfers() {
        return maximumConcurrentTransfers;
    }

    /**
     * Returns the immutable timeoutMillis value.
     *
     * @return timeoutMillis value
     */
    public long timeoutMillis() {
        return timeoutMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof HttpFileTransferLimits)) {
            return false;
        }
        HttpFileTransferLimits other = (HttpFileTransferLimits) value;
        return maximumPendingPerConnection == other.maximumPendingPerConnection
                && maximumConcurrentTransfers == other.maximumConcurrentTransfers
                && timeoutMillis == other.timeoutMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.lang.Integer.hashCode(maximumPendingPerConnection);
        result = 31 * result + java.lang.Integer.hashCode(maximumConcurrentTransfers);
        result = 31 * result + java.lang.Long.hashCode(timeoutMillis);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "HttpFileTransferLimits[maximumPendingPerConnection=" + maximumPendingPerConnection
                + ", maximumConcurrentTransfers=" + maximumConcurrentTransfers
                + ", timeoutMillis=" + timeoutMillis
                + "]";
    }
}
