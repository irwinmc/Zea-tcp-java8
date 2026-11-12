package com.akakata.server.http;

import io.netty.handler.codec.http.QueryStringDecoder;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Opens static files beneath a trusted root while rejecting traversal and symbolic links.
 *
 * <p>Secure directory streams anchor child traversal to directory handles where the
 * filesystem supports them. The portable fallback checks real containment and file
 * identity before and after opening without following the final link. Root parents
 * and directory structure must remain application-controlled on portable filesystems.
 *
 * @since 0.8.4
 */
public final class StaticResourceResolver {

    /**
     * Configured absolute root, independent of individual request paths.
     */
    private final Path root;

    /**
     * Open handle and metadata transferred to the HTTP response owner.
     *
     */
    public static final class Resource implements AutoCloseable {
        /**
         * Immutable value of channel.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("channel")
        private final FileChannel channel;

        /**
         * Immutable value of length.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("length")
        private final long length;

        /**
         * Immutable value of modified.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("modified")
        private final FileTime modified;

        /**
         * Immutable value of name.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("name")
        private final String name;


        /**
         * Closes the owned file handle idempotently.
         *
         * @throws IOException if handle closure fails
         */
        @Override
        public void close() throws IOException {
            channel.close();
        }

        /**
         * Creates the immutable value.
         *
         * @param channel value of channel
         * @param length value of length
         * @param modified value of modified
         * @param name value of name
         */
        @com.fasterxml.jackson.annotation.JsonCreator
        public Resource(@com.fasterxml.jackson.annotation.JsonProperty("channel") FileChannel channel, @com.fasterxml.jackson.annotation.JsonProperty("length") long length, @com.fasterxml.jackson.annotation.JsonProperty("modified") FileTime modified, @com.fasterxml.jackson.annotation.JsonProperty("name") String name) {
            this.channel = channel;
            this.length = length;
            this.modified = modified;
            this.name = name;
        }

        /**
         * Returns the immutable channel value.
         *
         * @return channel value
         */
        public FileChannel channel() {
            return channel;
        }

        /**
         * Returns the immutable length value.
         *
         * @return length value
         */
        public long length() {
            return length;
        }

        /**
         * Returns the immutable modified value.
         *
         * @return modified value
         */
        public FileTime modified() {
            return modified;
        }

        /**
         * Returns the immutable name value.
         *
         * @return name value
         */
        public String name() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object value) {
            if (this == value) {
                return true;
            }
            if (!(value instanceof Resource)) {
                return false;
            }
            Resource other = (Resource) value;
            return java.util.Objects.equals(channel, other.channel)
                    && length == other.length
                    && java.util.Objects.equals(modified, other.modified)
                    && java.util.Objects.equals(name, other.name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + java.util.Objects.hashCode(channel);
            result = 31 * result + java.lang.Long.hashCode(length);
            result = 31 * result + java.util.Objects.hashCode(modified);
            result = 31 * result + java.util.Objects.hashCode(name);
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Resource[channel=" + channel
                    + ", length=" + length
                    + ", modified=" + modified
                    + ", name=" + name
                    + "]";
        }
    }

    /**
     * Selects a root without requiring it to exist before startup.
     *
     * @param root trusted static directory
     */
    public StaticResourceResolver(Path root) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    /**
     * Validates a URI and returns one owned file handle.
     *
     * @param uri request URI, including optional query parameters
     * @return opened regular resource
     * @throws IOException if the path is forbidden, missing or cannot be safely opened
     */
    public Resource open(String uri) throws IOException {
        Path relative = relativePath(uri);
        if (Files.isSymbolicLink(root)) {
            throw new AccessDeniedException("Static root must not be a symbolic link");
        }
        Path realRoot = root.toRealPath();
        Resource resource = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(realRoot)) {
            SecureDirectoryStream<Path> java8Pattern36;
            if (((java8Pattern36 = com.akakata.compat.Java8.cast(stream, SecureDirectoryStream.class)) != null)) {
                resource = openSecure(java8Pattern36, relative);
            } else {
                resource = openPortable(realRoot, relative);
            }
            return resource;
        } catch (IOException | RuntimeException failure) {
            closeAfterFailure(resource, failure);
            throw failure;
        }
    }

    /**
     * Rejects decoded traversal, hidden segments and platform-dependent separators.
     *
     * @param uri request URI
     * @return relative path beneath the root
     * @throws AccessDeniedException if the URI violates the directory boundary
     */
    private static Path relativePath(String uri) throws AccessDeniedException {
        try {
            String path = new QueryStringDecoder(uri).path();
            if (!path.startsWith("/") || path.length() == 1 || path.indexOf('\\') >= 0) {
                throw new IllegalArgumentException("Invalid static path");
            }
            Path relative = java.nio.file.Paths.get(path.substring(1));
            if (relative.isAbsolute()) {
                throw new IllegalArgumentException("Absolute static path");
            }
            for (Path segment : relative) {
                if (segment.toString().startsWith(".")) {
                    throw new IllegalArgumentException("Hidden or traversal segment");
                }
            }
            return relative;
        } catch (IllegalArgumentException failure) {
            throw new AccessDeniedException("Invalid static resource path");
        }
    }

    /**
     * Walks child directories relative to open directory descriptors.
     *
     * @param root     open root directory
     * @param relative validated relative path
     * @return one owned resource
     * @throws IOException if traversal or opening fails
     */
    private static Resource openSecure(SecureDirectoryStream<Path> root, Path relative) throws IOException {
        List<SecureDirectoryStream<Path>> children = new ArrayList<>();
        Resource resource = null;
        try {
            try {
                SecureDirectoryStream<Path> directory = root;
                for (int index = 0; index < relative.getNameCount() - 1; index++) {
                    Path child = relative.getName(index);
                    BasicFileAttributes attributes = directory.getFileAttributeView(child, BasicFileAttributeView.class,
                            LinkOption.NOFOLLOW_LINKS).readAttributes();
                    if (attributes.isSymbolicLink()) {
                        throw new AccessDeniedException("Static symbolic links are forbidden");
                    }
                    if (!attributes.isDirectory()) {
                        throw new NoSuchFileException("Static parent is not a directory");
                    }
                    directory = directory.newDirectoryStream(child, LinkOption.NOFOLLOW_LINKS);
                    children.add(directory);
                }
                Path name = relative.getFileName();
                BasicFileAttributes attributes = directory.getFileAttributeView(name, BasicFileAttributeView.class,
                        LinkOption.NOFOLLOW_LINKS).readAttributes();
                requireRegular(attributes);
                java.nio.channels.SeekableByteChannel channel = directory.newByteChannel(name, com.akakata.compat.Java8.setOf(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
                FileChannel java8Pattern37;
                if (!(((java8Pattern37 = com.akakata.compat.Java8.cast(channel, FileChannel.class)) != null))) {
                    channel.close();
                    throw new IOException("Static file channel does not support file-region transfer");
                }
                try {
                    resource = new Resource(java8Pattern37, java8Pattern37.size(), attributes.lastModifiedTime(), name.toString());
                } catch (IOException | RuntimeException failure) {
                    java8Pattern37.close();
                    throw failure;
                }
            } finally {
                IOException failure = null;
                for (int index = children.size() - 1; index >= 0; index--) {
                    try {
                        children.get(index).close();
                    } catch (IOException problem) {
                        if (failure == null) {
                            failure = problem;
                        } else {
                            failure.addSuppressed(problem);
                        }
                    }
                }
                if (failure != null) {
                    throw failure;
                }
            }
            return resource;
        } catch (IOException | RuntimeException failure) {
            closeAfterFailure(resource, failure);
            throw failure;
        }
    }

    /**
     * Applies portable containment checks before and after a no-follow file open.
     *
     * @param realRoot canonical trusted root
     * @param relative validated relative path
     * @return one owned resource
     * @throws IOException if containment or file identity changes
     */
    private static Resource openPortable(Path realRoot, Path relative) throws IOException {
        Path path = checkedPath(realRoot, relative);
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        requireRegular(before);
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
        try {
            checkedPath(realRoot, relative);
            BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            requireRegular(after);
            if (!Objects.equals(before.fileKey(), after.fileKey())) {
                throw new AccessDeniedException("Static resource changed while opening");
            }
            return new Resource(channel, channel.size(), after.lastModifiedTime(), relative.getFileName().toString());
        } catch (IOException | RuntimeException failure) {
            channel.close();
            throw failure;
        }
    }

    /**
     * Checks every child component and the final canonical location.
     *
     * @param realRoot trusted canonical root
     * @param relative validated relative path
     * @return canonical contained file path
     * @throws IOException if a component is a link or escapes the root
     */
    private static Path checkedPath(Path realRoot, Path relative) throws IOException {
        Path path = realRoot;
        for (Path segment : relative) {
            path = path.resolve(segment);
            if (Files.isSymbolicLink(path)) {
                throw new AccessDeniedException("Static symbolic links are forbidden");
            }
        }
        Path real = path.toRealPath();
        if (!real.startsWith(realRoot)) {
            throw new AccessDeniedException("Static resource escapes its root");
        }
        return real;
    }

    /**
     * Limits transfer handles to regular files.
     *
     * @param attributes no-follow file attributes
     * @throws IOException if the entry is a link or not a regular file
     */
    private static void requireRegular(BasicFileAttributes attributes) throws IOException {
        if (attributes.isSymbolicLink()) {
            throw new AccessDeniedException("Static symbolic links are forbidden");
        }
        if (!attributes.isRegularFile()) {
            throw new NoSuchFileException("Static resource is not a regular file");
        }
    }

    /**
     * Closes an acquired handle when later validation or directory cleanup fails.
     *
     * @param resource possibly acquired resource
     * @param failure  original failure receiving suppressed close errors
     */
    private static void closeAfterFailure(Resource resource, Throwable failure) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException problem) {
                failure.addSuppressed(problem);
            }
        }
    }
}
