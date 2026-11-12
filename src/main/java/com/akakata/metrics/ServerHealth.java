package com.akakata.metrics;

import com.akakata.lifecycle.ServerLifecycle;

import java.util.Map;

/**
 * Supplies a fast, nonblocking application health snapshot to presentation layers.
 *
 * @since 0.8.4
 */
@FunctionalInterface
public interface ServerHealth {

    /**
     * Captures health without submitting tasks to the resources being checked.
     *
     * @return current lifecycle and availability results
     */
    Snapshot snapshot();

    /**
     * Immutable health result with named dependency checks.
     *
     */
    public static final class Snapshot {
        /**
         * Immutable value of phase.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("phase")
        private final ServerLifecycle.Phase phase;

        /**
         * Immutable value of live.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("live")
        private final boolean live;

        /**
         * Immutable value of ready.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("ready")
        private final boolean ready;

        /**
         * Immutable value of checks.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("checks")
        private final Map<String, Boolean> checks;


        /**
         * Copies the check results so consumers cannot mutate the snapshot.
         *
         * @param phase  application lifecycle phase
         * @param live   liveness result
         * @param ready  readiness result
         * @param checks named availability checks
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public Snapshot(@com.fasterxml.jackson.annotation.JsonProperty("phase") ServerLifecycle.Phase phase, @com.fasterxml.jackson.annotation.JsonProperty("live") boolean live, @com.fasterxml.jackson.annotation.JsonProperty("ready") boolean ready, @com.fasterxml.jackson.annotation.JsonProperty("checks") Map<String, Boolean> checks) {
            java.util.Objects.requireNonNull(phase, "phase");
            checks = com.akakata.compat.Java8.mapCopyOf(checks);

            this.phase = phase;
            this.live = live;
            this.ready = ready;
            this.checks = checks;
        }

        /**
         * Returns the immutable phase value.
         *
         * @return phase value
         */
        public ServerLifecycle.Phase phase() {
            return phase;
        }

        /**
         * Returns the immutable live value.
         *
         * @return live value
         */
        public boolean live() {
            return live;
        }

        /**
         * Returns the immutable ready value.
         *
         * @return ready value
         */
        public boolean ready() {
            return ready;
        }

        /**
         * Returns the immutable checks value.
         *
         * @return checks value
         */
        public Map<String, Boolean> checks() {
            return checks;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object value) {
            if (this == value) {
                return true;
            }
            if (!(value instanceof Snapshot)) {
                return false;
            }
            Snapshot other = (Snapshot) value;
            return java.util.Objects.equals(phase, other.phase)
                    && live == other.live
                    && ready == other.ready
                    && java.util.Objects.equals(checks, other.checks);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + java.util.Objects.hashCode(phase);
            result = 31 * result + java.lang.Boolean.hashCode(live);
            result = 31 * result + java.lang.Boolean.hashCode(ready);
            result = 31 * result + java.util.Objects.hashCode(checks);
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Snapshot[phase=" + phase
                    + ", live=" + live
                    + ", ready=" + ready
                    + ", checks=" + checks
                    + "]";
        }
    }
}
