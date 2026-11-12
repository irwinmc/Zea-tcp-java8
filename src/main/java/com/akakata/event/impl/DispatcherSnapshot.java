package com.akakata.event.impl;

/**
 * Captures one executor's admission budgets, outcomes and approximate timing percentiles.
 *
 */
public final class DispatcherSnapshot {
    /**
     * Immutable value of queued.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("queued")
    private final int queued;

    /**
     * Immutable value of pendingBytes.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("pendingBytes")
    private final long pendingBytes;

    /**
     * Immutable value of capacity.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("capacity")
    private final int capacity;

    /**
     * Immutable value of byteBudget.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("byteBudget")
    private final long byteBudget;

    /**
     * Immutable value of accepted.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("accepted")
    private final long accepted;

    /**
     * Immutable value of completed.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("completed")
    private final long completed;

    /**
     * Immutable value of cancelled.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("cancelled")
    private final long cancelled;

    /**
     * Immutable value of rejected.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("rejected")
    private final long rejected;

    /**
     * Immutable value of waitP95Micros.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("waitP95Micros")
    private final long waitP95Micros;

    /**
     * Immutable value of waitP99Micros.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("waitP99Micros")
    private final long waitP99Micros;

    /**
     * Immutable value of executionP95Micros.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("executionP95Micros")
    private final long executionP95Micros;

    /**
     * Immutable value of executionP99Micros.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("executionP99Micros")
    private final long executionP99Micros;

    /**
     * Creates the immutable value.
     *
     * @param queued value of queued
     * @param pendingBytes value of pendingBytes
     * @param capacity value of capacity
     * @param byteBudget value of byteBudget
     * @param accepted value of accepted
     * @param completed value of completed
     * @param cancelled value of cancelled
     * @param rejected value of rejected
     * @param waitP95Micros value of waitP95Micros
     * @param waitP99Micros value of waitP99Micros
     * @param executionP95Micros value of executionP95Micros
     * @param executionP99Micros value of executionP99Micros
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public DispatcherSnapshot(@com.fasterxml.jackson.annotation.JsonProperty("queued") int queued, @com.fasterxml.jackson.annotation.JsonProperty("pendingBytes") long pendingBytes, @com.fasterxml.jackson.annotation.JsonProperty("capacity") int capacity, @com.fasterxml.jackson.annotation.JsonProperty("byteBudget") long byteBudget, @com.fasterxml.jackson.annotation.JsonProperty("accepted") long accepted, @com.fasterxml.jackson.annotation.JsonProperty("completed") long completed, @com.fasterxml.jackson.annotation.JsonProperty("cancelled") long cancelled, @com.fasterxml.jackson.annotation.JsonProperty("rejected") long rejected, @com.fasterxml.jackson.annotation.JsonProperty("waitP95Micros") long waitP95Micros, @com.fasterxml.jackson.annotation.JsonProperty("waitP99Micros") long waitP99Micros, @com.fasterxml.jackson.annotation.JsonProperty("executionP95Micros") long executionP95Micros, @com.fasterxml.jackson.annotation.JsonProperty("executionP99Micros") long executionP99Micros) {
        this.queued = queued;
        this.pendingBytes = pendingBytes;
        this.capacity = capacity;
        this.byteBudget = byteBudget;
        this.accepted = accepted;
        this.completed = completed;
        this.cancelled = cancelled;
        this.rejected = rejected;
        this.waitP95Micros = waitP95Micros;
        this.waitP99Micros = waitP99Micros;
        this.executionP95Micros = executionP95Micros;
        this.executionP99Micros = executionP99Micros;
    }

    /**
     * Returns the immutable queued value.
     *
     * @return queued value
     */
    public int queued() {
        return queued;
    }

    /**
     * Returns the immutable pendingBytes value.
     *
     * @return pendingBytes value
     */
    public long pendingBytes() {
        return pendingBytes;
    }

    /**
     * Returns the immutable capacity value.
     *
     * @return capacity value
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the immutable byteBudget value.
     *
     * @return byteBudget value
     */
    public long byteBudget() {
        return byteBudget;
    }

    /**
     * Returns the immutable accepted value.
     *
     * @return accepted value
     */
    public long accepted() {
        return accepted;
    }

    /**
     * Returns the immutable completed value.
     *
     * @return completed value
     */
    public long completed() {
        return completed;
    }

    /**
     * Returns the immutable cancelled value.
     *
     * @return cancelled value
     */
    public long cancelled() {
        return cancelled;
    }

    /**
     * Returns the immutable rejected value.
     *
     * @return rejected value
     */
    public long rejected() {
        return rejected;
    }

    /**
     * Returns the immutable waitP95Micros value.
     *
     * @return waitP95Micros value
     */
    public long waitP95Micros() {
        return waitP95Micros;
    }

    /**
     * Returns the immutable waitP99Micros value.
     *
     * @return waitP99Micros value
     */
    public long waitP99Micros() {
        return waitP99Micros;
    }

    /**
     * Returns the immutable executionP95Micros value.
     *
     * @return executionP95Micros value
     */
    public long executionP95Micros() {
        return executionP95Micros;
    }

    /**
     * Returns the immutable executionP99Micros value.
     *
     * @return executionP99Micros value
     */
    public long executionP99Micros() {
        return executionP99Micros;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof DispatcherSnapshot)) {
            return false;
        }
        DispatcherSnapshot other = (DispatcherSnapshot) value;
        return queued == other.queued
                && pendingBytes == other.pendingBytes
                && capacity == other.capacity
                && byteBudget == other.byteBudget
                && accepted == other.accepted
                && completed == other.completed
                && cancelled == other.cancelled
                && rejected == other.rejected
                && waitP95Micros == other.waitP95Micros
                && waitP99Micros == other.waitP99Micros
                && executionP95Micros == other.executionP95Micros
                && executionP99Micros == other.executionP99Micros;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.lang.Integer.hashCode(queued);
        result = 31 * result + java.lang.Long.hashCode(pendingBytes);
        result = 31 * result + java.lang.Integer.hashCode(capacity);
        result = 31 * result + java.lang.Long.hashCode(byteBudget);
        result = 31 * result + java.lang.Long.hashCode(accepted);
        result = 31 * result + java.lang.Long.hashCode(completed);
        result = 31 * result + java.lang.Long.hashCode(cancelled);
        result = 31 * result + java.lang.Long.hashCode(rejected);
        result = 31 * result + java.lang.Long.hashCode(waitP95Micros);
        result = 31 * result + java.lang.Long.hashCode(waitP99Micros);
        result = 31 * result + java.lang.Long.hashCode(executionP95Micros);
        result = 31 * result + java.lang.Long.hashCode(executionP99Micros);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DispatcherSnapshot[queued=" + queued
                + ", pendingBytes=" + pendingBytes
                + ", capacity=" + capacity
                + ", byteBudget=" + byteBudget
                + ", accepted=" + accepted
                + ", completed=" + completed
                + ", cancelled=" + cancelled
                + ", rejected=" + rejected
                + ", waitP95Micros=" + waitP95Micros
                + ", waitP99Micros=" + waitP99Micros
                + ", executionP95Micros=" + executionP95Micros
                + ", executionP99Micros=" + executionP99Micros
                + "]";
    }
}
