package com.akakata.app;

/**
 * Reports admission of an explicit Game send to recipient session queues.
 *
 * <p>Acceptance is not a network delivery acknowledgment. Closed or disconnected
 * recipients are skipped; overloaded recipients are rejected and closed by the
 * default Game so later state updates cannot follow a missing update.
 *
 */
public final class DeliveryReport {
    /**
     * Immutable value of accepted.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("accepted")
    private final int accepted;

    /**
     * Immutable value of rejected.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("rejected")
    private final int rejected;

    /**
     * Immutable value of skipped.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("skipped")
    private final int skipped;


    /**
     * Validates the nonnegative recipient counts.
     *
     * @param accepted the accepted recipient count
     * @param rejected the rejected recipient count
     * @param skipped  the skipped recipient count
     * @throws IllegalArgumentException if any count is negative
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public DeliveryReport(@com.fasterxml.jackson.annotation.JsonProperty("accepted") int accepted, @com.fasterxml.jackson.annotation.JsonProperty("rejected") int rejected, @com.fasterxml.jackson.annotation.JsonProperty("skipped") int skipped) {
        if (accepted < 0 || rejected < 0 || skipped < 0) {
            throw new IllegalArgumentException("Recipient counts must be nonnegative");
        }

        this.accepted = accepted;
        this.rejected = rejected;
        this.skipped = skipped;
    }

    /**
     * Returns the immutable accepted value.
     *
     * @return accepted value
     */
    public int accepted() {
        return accepted;
    }

    /**
     * Returns the immutable rejected value.
     *
     * @return rejected value
     */
    public int rejected() {
        return rejected;
    }

    /**
     * Returns the immutable skipped value.
     *
     * @return skipped value
     */
    public int skipped() {
        return skipped;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof DeliveryReport)) {
            return false;
        }
        DeliveryReport other = (DeliveryReport) value;
        return accepted == other.accepted
                && rejected == other.rejected
                && skipped == other.skipped;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.lang.Integer.hashCode(accepted);
        result = 31 * result + java.lang.Integer.hashCode(rejected);
        result = 31 * result + java.lang.Integer.hashCode(skipped);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DeliveryReport[accepted=" + accepted
                + ", rejected=" + rejected
                + ", skipped=" + skipped
                + "]";
    }
}
