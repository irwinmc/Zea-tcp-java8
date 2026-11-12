package com.akakata.compat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Supplies equivalent contracts for JDK APIs unavailable on Java 8.
 * This implementation utility does not change framework admission or dispatch.
 */
public final class Java8 {

    /**
     * Prevents construction of this utility.
     */
    private Java8() {
    }

    /**
     * Captures a pattern operand once and returns its matching type.
     *
     * @param value evaluated operand
     * @param type tested reference type
     * @param <T> matching type
     * @return typed value, or null when the pattern does not match
     */
    public static <T> T cast(Object value, Class<T> type) {
        return type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * Creates an immutable, null-rejecting list in encounter order.
     *
     * @param values elements to snapshot
     * @param <T> element type
     * @return immutable list
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... values) {
        T[] copy = values.clone();
        for (T value : copy) {
            Objects.requireNonNull(value);
        }
        return Collections.unmodifiableList(new NullRejectingList<>(Arrays.asList(copy)));
    }

    /**
     * Copies a collection into an immutable, null-rejecting list.
     *
     * @param values source collection
     * @param <T> element type
     * @return immutable snapshot
     */
    public static <T> List<T> listCopyOf(Collection<? extends T> values) {
        List<T> copy = new ArrayList<>(values.size());
        for (T value : values) {
            copy.add(Objects.requireNonNull(value));
        }
        return Collections.unmodifiableList(new NullRejectingList<>(copy));
    }

    /**
     * Creates an immutable set, rejecting nulls and duplicate elements.
     *
     * @param values elements to snapshot
     * @param <T> element type
     * @return immutable set
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... values) {
        Set<T> copy = new LinkedHashSet<>();
        for (T value : values) {
            if (!copy.add(Objects.requireNonNull(value))) {
                throw new IllegalArgumentException("duplicate element: " + value);
            }
        }
        return Collections.unmodifiableSet(new NullRejectingSet<>(copy));
    }

    /**
     * Copies a collection into an immutable set, rejecting nulls.
     *
     * @param values elements to snapshot
     * @param <T> element type
     * @return immutable set with duplicates collapsed
     */
    public static <T> Set<T> setCopyOf(Collection<? extends T> values) {
        Set<T> copy = new LinkedHashSet<>();
        for (T value : values) {
            copy.add(Objects.requireNonNull(value));
        }
        return Collections.unmodifiableSet(new NullRejectingSet<>(copy));
    }

    /**
     * Creates an immutable map from alternating keys and values.
     *
     * @param pairs alternating keys and values
     * @param <K> key type
     * @param <V> value type
     * @return null-rejecting immutable map with unique keys
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapOf(Object... pairs) {
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("Keys and values must be paired");
        }
        Map<K, V> copy = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            K key = (K) Objects.requireNonNull(pairs[index]);
            V value = (V) Objects.requireNonNull(pairs[index + 1]);
            if (copy.containsKey(key)) {
                throw new IllegalArgumentException("duplicate key: " + key);
            }
            copy.put(key, value);
        }
        return Collections.unmodifiableMap(new NullRejectingMap<>(copy));
    }

    /**
     * Preserves target typing for a single map entry containing a lambda.
     *
     * @param key nonnull key
     * @param value nonnull value
     * @param <K> key type
     * @param <V> value type
     * @return immutable singleton map
     */
    public static <K, V> Map<K, V> mapOf(K key, V value) {
        return Java8.<K, V>mapOf(new Object[]{key, value});
    }

    /**
     * Preserves a spin-loop processor hint as a no-op on Java 8.
     */
    public static void onSpinWait() {
    }

    /**
     * Observes virtual-thread identity when that optional runtime API exists.
     *
     * @param thread thread to inspect
     * @return virtual-thread status, or false on runtimes without virtual threads
     */
    public static boolean isVirtual(Thread thread) {
        Objects.requireNonNull(thread);
        try {
            return (Boolean) Thread.class.getMethod("isVirtual").invoke(thread);
        } catch (NoSuchMethodException unavailable) {
            return false;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot inspect thread identity", failure);
        }
    }

    /**
     * Copies a map into an immutable, null-rejecting snapshot.
     *
     * @param values source map
     * @param <K> key type
     * @param <V> value type
     * @return immutable map
     */
    public static <K, V> Map<K, V> mapCopyOf(Map<? extends K, ? extends V> values) {
        Map<K, V> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key), Objects.requireNonNull(value)));
        return Collections.unmodifiableMap(new NullRejectingMap<>(copy));
    }

    /**
     * Collects stream elements into an immutable list while permitting nulls.
     *
     * @param <T> element type
     * @return ordered immutable-list collector
     */
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }

    /**
     * Tests the Unicode whitespace set used by the upstream JDK.
     *
     * @param value string to test
     * @return whether the string is empty or contains only whitespace
     */
    public static boolean isBlank(String value) {
        return value.codePoints().allMatch(Java8::isWhitespace);
    }

    /**
     * Matches upstream whitespace independently of the Java 8 Unicode database.
     *
     * @param codePoint Unicode code point
     * @return whether the code point is Java whitespace
     */
    private static boolean isWhitespace(int codePoint) {
        return codePoint >= 9 && codePoint <= 13 || codePoint >= 28 && codePoint <= 32
                || codePoint == 0x1680 || codePoint >= 0x2000 && codePoint <= 0x2006
                || codePoint >= 0x2008 && codePoint <= 0x200A || codePoint == 0x2028
                || codePoint == 0x2029 || codePoint == 0x205F || codePoint == 0x3000;
    }

    /**
     * Repeats a string without using a post-Java-8 API.
     *
     * @param value repeated string
     * @param count nonnegative repetition count
     * @return repeated string
     */
    public static String repeat(String value, int count) {
        Objects.requireNonNull(value);
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        long length = (long) value.length() * count;
        if (length > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        StringBuilder result = new StringBuilder((int) length);
        for (int index = 0; index < count; index++) {
            result.append(value);
        }
        return result.toString();
    }

    /**
     * Checks a byte-array slice without overflowing index arithmetic.
     *
     * @param offset start index
     * @param length requested length
     * @param size array size
     * @return valid offset
     */
    public static int checkFromIndexSize(int offset, int length, int size) {
        if (offset < 0 || length < 0 || size < 0 || offset > size - length) {
            throw new IndexOutOfBoundsException("Invalid array slice");
        }
        return offset;
    }

    /**
     * Creates an already exceptionally completed future.
     *
     * @param failure nonnull completion failure
     * @param <T> result type
     * @return failed future
     */
    public static <T> CompletableFuture<T> failedFuture(Throwable failure) {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(Objects.requireNonNull(failure));
        return result;
    }

    /**
     * Copies completion without allowing observers to cancel the source future.
     *
     * @param source actual operation completion
     * @param <T> result type
     * @return independent completion observer
     */
    public static <T> CompletableFuture<T> copy(CompletableFuture<T> source) {
        return source.thenApply(value -> value);
    }

    /**
     * Creates independent named threads with the requested daemon policy.
     *
     * @param prefix thread-name prefix
     * @param initial initial suffix
     * @param daemon whether threads are daemon threads
     * @return named thread factory
     */
    public static ThreadFactory threadFactory(String prefix, long initial, boolean daemon) {
        AtomicLong sequence = new AtomicLong(initial);
        return action -> {
            Thread thread = new Thread(action, prefix + sequence.getAndIncrement());
            thread.setDaemon(daemon);
            return thread;
        };
    }

    /**
     * Starts an independent daemon thread for asynchronous cleanup or a test actor.
     *
     * @param name thread name
     * @param action asynchronous action
     * @return started thread
     */
    public static Thread startDaemon(String name, Runnable action) {
        Thread thread = new Thread(action, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Returns a list's first element with the upstream empty-list exception type.
     *
     * @param values source list
     * @param <T> element type
     * @return first element
     */
    public static <T> T first(List<T> values) {
        if (values.isEmpty()) {
            throw new NoSuchElementException();
        }
        return values.get(0);
    }

    /**
     * Reads at most the requested byte count, preserving a short EOF result.
     *
     * @param input source stream
     * @param length requested length
     * @return bytes read before the requested count or EOF
     * @throws IOException if the stream fails
     */
    public static byte[] readNBytes(InputStream input, int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("length is negative");
        }
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(bytes, offset, length - offset);
            if (count < 0) {
                break;
            }
            if (count == 0) {
                int value = input.read();
                if (value < 0) {
                    break;
                }
                bytes[offset++] = (byte) value;
            } else {
                offset += count;
            }
        }
        return offset == length ? bytes : Arrays.copyOf(bytes, offset);
    }

    /**
     * Reads UTF-8 text, rejecting malformed input as the upstream Files API does.
     *
     * @param path source file
     * @return decoded text
     * @throws IOException if reading or decoding fails
     */
    public static String readString(Path path) throws IOException {
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(path))).toString();
    }

    /**
     * Writes UTF-8 text using the same default creation and truncation options.
     *
     * @param path destination file
     * @param text source characters
     * @param options file-open options
     * @return destination path
     * @throws IOException if encoding or writing fails
     */
    public static Path writeString(Path path, CharSequence text, OpenOption... options) throws IOException {
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(text));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return Files.write(path, bytes, options);
    }

    /**
     * Adapts an executor to the upstream try-with-resources closure contract.
     *
     * @param delegate owned executor
     * @return closeable executor adapter
     */
    public static ClosingExecutor closingExecutor(ExecutorService delegate) {
        return new ClosingExecutor(delegate);
    }

    /**
     * Retains null-query rejection for null-free immutable lists.
     *
     * @param <T> element type
     */
    private static final class NullRejectingList<T> extends AbstractList<T>
            implements RandomAccess, java.io.Serializable {

        /**
         * Serialization identity of this list adapter.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Privately owned element snapshot.
         */
        private final List<T> values;

        /**
         * Creates a null-query adapter around a privately owned snapshot.
         *
         * @param values privately owned nonnull elements
         */
        private NullRejectingList(List<T> values) {
            this.values = values;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T get(int index) {
            return values.get(index);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return values.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object value) {
            return values.contains(Objects.requireNonNull(value));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int indexOf(Object value) {
            return values.indexOf(Objects.requireNonNull(value));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int lastIndexOf(Object value) {
            return values.lastIndexOf(Objects.requireNonNull(value));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<T> subList(int start, int end) {
            return Collections.unmodifiableList(new NullRejectingList<>(values.subList(start, end)));
        }
    }

    /**
     * Retains null-query rejection for null-free immutable sets.
     *
     * @param <T> element type
     */
    private static final class NullRejectingSet<T> extends AbstractSet<T> implements java.io.Serializable {

        /**
         * Serialization identity of this set adapter.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Privately owned element snapshot.
         */
        private final Set<T> values;

        /**
         * Creates a null-query adapter around a privately owned snapshot.
         *
         * @param values privately owned nonnull elements
         */
        private NullRejectingSet(Set<T> values) {
            this.values = values;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<T> iterator() {
            return values.iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            return values.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean contains(Object value) {
            return values.contains(Objects.requireNonNull(value));
        }
    }

    /**
     * Retains null-query rejection for null-free immutable maps.
     *
     * @param <K> key type
     * @param <V> value type
     */
    private static final class NullRejectingMap<K, V> extends AbstractMap<K, V> implements java.io.Serializable {

        /**
         * Serialization identity of this map adapter.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Privately owned entry snapshot.
         */
        private final Map<K, V> values;

        /**
         * Creates a null-query adapter around a privately owned snapshot.
         *
         * @param values privately owned nonnull entries
         */
        private NullRejectingMap(Map<K, V> values) {
            this.values = values;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Entry<K, V>> entrySet() {
            return values.entrySet();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public V get(Object key) {
            return values.get(Objects.requireNonNull(key));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean containsKey(Object key) {
            return values.containsKey(Objects.requireNonNull(key));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean containsValue(Object value) {
            return values.containsValue(Objects.requireNonNull(value));
        }
    }

    /**
     * Waits for executor termination on close and preserves interruption.
     */
    public static final class ClosingExecutor extends AbstractExecutorService implements AutoCloseable {

        /**
         * Executor retaining all task submission and execution ownership.
         */
        private final ExecutorService delegate;

        /**
         * Creates a closeable ownership adapter.
         *
         * @param delegate owned executor
         */
        private ClosingExecutor(ExecutorService delegate) {
            this.delegate = Objects.requireNonNull(delegate);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(Runnable action) {
            delegate.execute(action);
        }

        /**
         * Closes the owned executor, cancelling remaining work after interruption.
         */
        @Override
        public void close() {
            delegate.shutdown();
            boolean interrupted = false;
            while (!delegate.isTerminated()) {
                try {
                    delegate.awaitTermination(1, TimeUnit.DAYS);
                } catch (InterruptedException failure) {
                    if (!interrupted) {
                        delegate.shutdownNow();
                    }
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
