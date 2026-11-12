package com.akakata.compat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Configures Java 8 JSON libraries to retain the synchronized Jackson 3 defaults.
 */
public final class Json8 {

    /**
     * Prevents construction of this utility.
     */
    private Json8() {
    }

    /**
     * Creates a builder with the upstream bean, null, trailing-token and time rules.
     *
     * @return independently configurable mapper builder
     */
    public static JsonMapper.Builder builder() {
        return JsonMapper.builder()
                .enable(com.fasterxml.jackson.core.json.JsonWriteFeature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
                .addModule(new Jdk8Module())
                .addModule(new JavaTimeModule())
                .defaultDateFormat(new LegacyDateFormat())
                .disable(MapperFeature.USE_GETTERS_AS_SETTERS,
                        MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS,
                        MapperFeature.DEFAULT_VIEW_INCLUSION)
                .enable(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES,
                        MapperFeature.SORT_PROPERTIES_ALPHABETICALLY,
                        MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX,
                        MapperFeature.USE_STD_BEAN_NAMING)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS,
                        SerializationFeature.FAIL_ON_ORDER_MAP_BY_INCOMPARABLE_KEY,
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                        DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    /**
     * Creates an upstream-compatible default mapper.
     *
     * @return independently owned mapper
     */
    public static ObjectMapper mapper() {
        return builder().build();
    }

    /**
     * Preserves the unchecked serialization failure contract of Jackson 3.
     *
     * @param mapper configured mapper
     * @param output borrowed output stream
     * @param value serialized value
     */
    public static void writeValue(ObjectMapper mapper, OutputStream output, Object value) {
        try {
            mapper.writeValue(output, value);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /**
     * Serializes a detached JSON payload with unchecked failure propagation.
     *
     * @param mapper configured mapper
     * @param value serialized value
     * @return JSON UTF-8 bytes
     */
    public static byte[] writeValueAsBytes(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /**
     * Serializes text with the upstream unchecked failure contract.
     *
     * @param mapper configured mapper
     * @param value serialized value
     * @return JSON text
     */
    public static String writeValueAsString(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /**
     * Preserves unchecked JSON parsing failures while borrowing the input stream.
     *
     * @param mapper configured mapper
     * @param input borrowed input stream
     * @param type target value type
     * @param <T> decoded type
     * @return decoded value
     */
    public static <T> T readValue(ObjectMapper mapper, InputStream input, TypeReference<T> type) {
        try {
            return mapper.readValue(input, type);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /**
     * Preserves the upstream millisecond ISO date format and zero-offset Z suffix.
     */
    private static final class LegacyDateFormat extends DateFormat {

        /**
         * Serialization identity of this date-format adapter.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Initializes the cloneable timezone and numeric-format state.
         */
        private LegacyDateFormat() {
            calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
            numberFormat = NumberFormat.getIntegerInstance(Locale.ROOT);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StringBuffer format(Date date, StringBuffer output, FieldPosition position) {
            new com.fasterxml.jackson.databind.util.StdDateFormat()
                    .withTimeZone(getTimeZone()).withLenient(isLenient()).format(date, output, position);
            int length = output.length();
            if (length >= 6 && output.substring(length - 6).equals("+00:00")) {
                output.replace(length - 6, length, "Z");
            }
            return output;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Date parse(String source, ParsePosition position) {
            return new com.fasterxml.jackson.databind.util.StdDateFormat()
                    .withTimeZone(getTimeZone()).withLenient(isLenient()).parse(source, position);
        }
    }
}
