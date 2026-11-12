package com.akakata.compat;

import com.akakata.event.Events;
import com.akakata.handlers.codec.EventFrames;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CancellationException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks adapters against inherited contracts and captured upstream JSON bytes.
 */
class Java8CompatibilityRegressionTest {

    /**
     * Creates the compatibility fixture.
     */
    Java8CompatibilityRegressionTest() {
    }

    /**
     * Loads JSON expectations captured from the verified Java 21 upstream snapshot.
     *
     * @return input values and independently captured JSON expectations
     * @throws Exception if a fixture or copied record cannot be loaded
     */
    static Stream<Arguments> wireCases() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Java8CompatibilityRegressionTest.class.getResourceAsStream(
                        "/java8/upstream-json-corpus.tsv")), StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                String[] parts = line.split("\t", 2);
                expected.put(parts[0], new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8));
            }
        }
        return samples().entrySet().stream().map(entry ->
                Arguments.of(entry.getKey(), entry.getValue(), Objects.requireNonNull(expected.get(entry.getKey()))));
    }

    /**
     * Builds inputs used by the separately captured upstream serialization corpus.
     *
     * @return deterministic source values
     * @throws Exception if a copied value type cannot be constructed
     */
    private static Map<String, Object> samples() throws Exception {
        Map<String, Object> samples = new LinkedHashMap<>();
        Map<String, Object> ordered = new LinkedHashMap<>();
        ordered.put("zulu", "中文🙂 \\\"\n\t");
        ordered.put("alpha", Arrays.asList(42, true, null));
        ordered.put("empty", null);
        samples.put("ordered-map", ordered);
        samples.put("bean-order-and-acronym", new Bean());
        samples.put("legacy-date", new Date(0));
        samples.put("proleptic-date", Date.from(Instant.parse("1490-01-02T03:04:05Z")));
        samples.put("bce-date", Date.from(Instant.parse("-0001-01-02T03:04:05Z")));
        samples.put("expanded-year-date", Date.from(Instant.parse("+10000-01-02T03:04:05Z")));
        samples.put("instant", Instant.parse("2024-01-02T03:04:05.123456789Z"));
        samples.put("local-date", LocalDate.of(2024, 2, 29));
        samples.put("local-date-time", LocalDateTime.of(2024, 2, 29, 3, 4, 5));
        samples.put("offset-date-time", OffsetDateTime.parse("2024-02-29T03:04:05+08:00"));
        samples.put("duration", Duration.ofMillis(1234));
        samples.put("optional", Optional.of("value"));
        samples.put("optional-empty", Optional.empty());
        samples.put("optional-int", OptionalInt.of(7));
        samples.put("decimal", new BigDecimal("1.234567890E+40"));
        samples.put("integer", new BigInteger("123456789012345678901234567890"));
        samples.put("nonfinite-number", Double.NaN);
        samples.put("char-array", new char[]{'a', '中'});
        samples.put("byte-array", new byte[]{0, 1, -1});
        samples.put("delivery-report", Class.forName("com.akakata.app.DeliveryReport")
                .getConstructor(int.class, int.class, int.class).newInstance(1, 2, 3));
        Object report = samples.get("delivery-report");
        samples.put("delivery-report-value-methods", report.toString() + ":" + report.hashCode());
        return samples;
    }

    /**
     * Retains exact application payload bytes, including bean names, records and dates.
     *
     * @param name corpus case name
     * @param value event payload
     * @param expected JSON captured from upstream
     */
    @ParameterizedTest
    @MethodSource("wireCases")
    void wirePayloadsMatchUpstream(String name, Object value, String expected) {
        ByteBuf frame = EventFrames.json(UnpooledByteBufAllocator.DEFAULT, Events.networkEvent(value));
        try {
            assertEquals(Events.NETWORK_MESSAGE, frame.readUnsignedByte(), name);
            byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
            assertEquals(bytes.length, frame.readInt(), name);
            byte[] actual = new byte[frame.readableBytes()];
            frame.readBytes(actual);
            assertArrayEquals(bytes, actual, name);
        } finally {
            frame.release();
        }
    }

    /**
     * Preserves factory null/duplicate rejection and snapshot mutation isolation.
     */
    @Test
    void immutableFactoriesPreserveAdmissionAndQueryContracts() {
        List<String> original = new ArrayList<>(Arrays.asList("one", "two"));
        List<String> copy = Java8.listCopyOf(original);
        original.clear();
        assertEquals(Arrays.asList("one", "two"), copy);
        assertThrows(NullPointerException.class, () -> Java8.listOf("one", null));
        assertThrows(NullPointerException.class, () -> copy.contains(null));
        assertThrows(NullPointerException.class, () -> copy.subList(0, 1).indexOf(null));
        assertThrows(UnsupportedOperationException.class, () -> copy.addAll(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> Java8.setOf("one", "one"));
        assertThrows(NullPointerException.class, () -> Java8.setOf("one").contains(null));
        assertThrows(IllegalArgumentException.class, () -> Java8.mapOf("key", 1, "key", 2));
        assertThrows(NullPointerException.class, () -> Java8.mapOf("key", 1).get(null));
        assertThrows(UnsupportedOperationException.class,
                () -> Java8.mapOf("key", 1).entrySet().iterator().next().setValue(2));
    }

    /**
     * Retains null elements and encounter order for stream-to-list adaptation.
     */
    @Test
    void streamListsPreserveNullValuesAndImmutability() {
        List<String> values = Stream.of("one", null, "two").collect(Java8.toUnmodifiableList());
        assertEquals(Arrays.asList("one", null, "two"), values);
        assertThrows(UnsupportedOperationException.class, () -> values.set(0, "changed"));
    }

    /**
     * Prevents an observer's cancellation from changing actual completion ownership.
     */
    @Test
    void completionCopiesPreserveIndependentCancellationAndFailureWrapping() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> observer = Java8.copy(source);
        assertTrue(observer.cancel(false));
        assertFalse(source.isDone());
        source.complete("done");
        assertEquals("done", source.join());
        CompletableFuture<String> cancelled = new CompletableFuture<>();
        cancelled.cancel(false);
        CompletableFuture<String> copied = Java8.copy(cancelled);
        assertFalse(copied.isCancelled());
        CompletionException failure = assertThrows(CompletionException.class, copied::join);
        assertInstanceOf(CancellationException.class, failure.getCause());
    }

    /**
     * Preserves Unicode whitespace rather than substituting ASCII trimming.
     */
    @Test
    void blankStringsMatchUpstreamUnicodeRules() {
        assertTrue(Java8.isBlank("\u2000\u2008\u3000\t"));
        assertFalse(Java8.isBlank("\u00a0"));
        assertFalse(Java8.isBlank("\u180e"));
        assertFalse(Java8.isBlank(" x "));
    }

    /**
     * Exercises alphabetic property ordering and acronym naming across JSON versions.
     */
    public static final class Bean {

        /**
         * Creates the bean-naming fixture.
         */
        public Bean() {
        }

        /**
         * Returns the final alphabetical property.
         *
         * @return final property value
         */
        public String getZulu() {
            return "last";
        }

        /**
         * Returns an alphabetical property.
         *
         * @return property value
         */
        public String getAlpha() {
            return "first";
        }

        /**
         * Returns an acronym-named property.
         *
         * @return acronym property value
         */
        public String getURL() {
            return "https://example.test/a";
        }

        /**
         * Returns the boolean property.
         *
         * @return enabled value
         */
        public boolean isEnabled() {
            return true;
        }
    }
}
