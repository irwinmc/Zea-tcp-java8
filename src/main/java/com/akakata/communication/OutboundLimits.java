package com.akakata.communication;

/**
 * Bounds socket writes before encoding and encoded buffers awaiting completion.
 *
 */
public final class OutboundLimits {
    /**
     * Immutable value of maximumPendingWrites.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumPendingWrites")
    private final int maximumPendingWrites;

    /**
     * Immutable value of maximumPendingBytes.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumPendingBytes")
    private final int maximumPendingBytes;

    /**
     * Immutable value of maximumPendingPayloadBytes.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("maximumPendingPayloadBytes")
    private final long maximumPendingPayloadBytes;


    /**
     * Default per-connection admission and encoded-buffer budgets.
     */
    public static final OutboundLimits DEFAULT = new OutboundLimits(1_024, 1_048_576, 1_048_576);

    /**
     * Creates legacy count and encoded-byte limits with a 1 MiB source-payload budget.
     *
     * @param maximumPendingWrites the positive pending-write allowance
     * @param maximumPendingBytes  the positive encoded-byte allowance
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public OutboundLimits(@com.fasterxml.jackson.annotation.JsonProperty("maximumPendingWrites") int maximumPendingWrites, @com.fasterxml.jackson.annotation.JsonProperty("maximumPendingBytes") int maximumPendingBytes) {
        this(maximumPendingWrites, maximumPendingBytes, 1_048_576);
    }

    /**
     * Validates positive write and byte budgets.
     *
     * @param maximumPendingWrites       the maximum pending application writes
     * @param maximumPendingBytes        the maximum pending encoded bytes
     * @param maximumPendingPayloadBytes the maximum pending source estimates
     * @throws IllegalArgumentException if either budget is not positive
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public OutboundLimits(@com.fasterxml.jackson.annotation.JsonProperty("maximumPendingWrites") int maximumPendingWrites, @com.fasterxml.jackson.annotation.JsonProperty("maximumPendingBytes") int maximumPendingBytes, @com.fasterxml.jackson.annotation.JsonProperty("maximumPendingPayloadBytes") long maximumPendingPayloadBytes) {
        if (maximumPendingWrites <= 0 || maximumPendingBytes <= 0 || maximumPendingPayloadBytes <= 0) {
            throw new IllegalArgumentException("Outbound budgets must be positive");
        }

        this.maximumPendingWrites = maximumPendingWrites;
        this.maximumPendingBytes = maximumPendingBytes;
        this.maximumPendingPayloadBytes = maximumPendingPayloadBytes;
    }

    /**
     * Returns the immutable maximumPendingWrites value.
     *
     * @return maximumPendingWrites value
     */
    public int maximumPendingWrites() {
        return maximumPendingWrites;
    }

    /**
     * Returns the immutable maximumPendingBytes value.
     *
     * @return maximumPendingBytes value
     */
    public int maximumPendingBytes() {
        return maximumPendingBytes;
    }

    /**
     * Returns the immutable maximumPendingPayloadBytes value.
     *
     * @return maximumPendingPayloadBytes value
     */
    public long maximumPendingPayloadBytes() {
        return maximumPendingPayloadBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof OutboundLimits)) {
            return false;
        }
        OutboundLimits other = (OutboundLimits) value;
        return maximumPendingWrites == other.maximumPendingWrites
                && maximumPendingBytes == other.maximumPendingBytes
                && maximumPendingPayloadBytes == other.maximumPendingPayloadBytes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.lang.Integer.hashCode(maximumPendingWrites);
        result = 31 * result + java.lang.Integer.hashCode(maximumPendingBytes);
        result = 31 * result + java.lang.Long.hashCode(maximumPendingPayloadBytes);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "OutboundLimits[maximumPendingWrites=" + maximumPendingWrites
                + ", maximumPendingBytes=" + maximumPendingBytes
                + ", maximumPendingPayloadBytes=" + maximumPendingPayloadBytes
                + "]";
    }
}
