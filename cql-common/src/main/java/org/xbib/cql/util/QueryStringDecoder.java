package org.xbib.cql.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits an HTTP query string into a path string and key-value parameter pairs.
 * This decoder is for one time use only.  Create a new instance for each URI:
 * <pre>
 * {@link QueryStringDecoder} decoder = new {@link QueryStringDecoder}("/hello?recipient=world&amp;x=1;y=2");
 * assert decoder.getPath().equals("/hello");
 * assert decoder.getParameters().get("recipient").get(0).equals("world");
 * assert decoder.getParameters().get("x").get(0).equals("1");
 * assert decoder.getParameters().get("y").get(0).equals("2");
 * </pre>
 * This decoder can also decode the content of an HTTP POST request whose
 * content type is {@code application/x-www-form-urlencoded}:
 * <pre>
 * {@link QueryStringDecoder} decoder = new {@link QueryStringDecoder}("recipient=world&amp;x=1;y=2", false);
 * </pre>
 * HashDOS vulnerability fix: As a workaround to the <a href="http://netty.io/s/hashdos">HashDOS</a> vulnerability,
 * the decoder limits the maximum number of decoded key-value parameter pairs, up to {@literal 1024} by
 * default, and you can configure it when you construct the decoder by passing an additional
 * integer parameter.
 */
public class QueryStringDecoder {

    private static final int DEFAULT_MAX_PARAMS = 1024;

    private final Charset charset;

    private final String uri;

    private final boolean hasPath;

    private final int maxParams;

    private String path;

    private Map<String, List<String>> params;

    private int nParams;

    /**
     * Creates a new decoder that decodes the specified URI. The decoder will
     * assume that the query string is encoded in UTF-8.
     * @param uri URI
     */
    public QueryStringDecoder(String uri) {
        this(uri, StandardCharsets.UTF_8);
    }

    /**
     * Creates a new decoder that decodes the specified URI encoded in the
     * specified charset.
     * @param uri URI
     * @param hasPath if path is provided
     */
    public QueryStringDecoder(String uri, boolean hasPath) {
        this(uri, StandardCharsets.UTF_8, hasPath);
    }

    /**
     * Creates a new decoder that decodes the specified URI encoded in the
     * specified charset.
     * @param uri URI
     * @param charset character set
     */
    public QueryStringDecoder(String uri, Charset charset) {
        this(uri, charset, true);
    }

    /**
     * Creates a new decoder that decodes the specified URI encoded in the
     * specified charset.
     * @param uri URI
     * @param charset character set
     * @param hasPath if path is provided
     */
    public QueryStringDecoder(String uri, Charset charset, boolean hasPath) {
        this(uri, charset, hasPath, DEFAULT_MAX_PARAMS);
    }

    /**
     * Creates a new decoder that decodes the specified URI encoded in the
     * specified charset.
     * @param uri URI
     * @param charset character set
     * @param hasPath if path is provided
     * @param maxParams maximum parameters
     */
    public QueryStringDecoder(String uri, Charset charset, boolean hasPath, int maxParams) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (maxParams <= 0) {
            throw new IllegalArgumentException(
                    "maxParams: " + maxParams + " (expected: a positive integer)");
        }

        this.uri = uri;
        this.charset = charset;
        this.maxParams = maxParams;
        this.hasPath = hasPath;
    }

    /**
     * Creates a new decoder that decodes the specified URI. The decoder will
     * assume that the query string is encoded in UTF-8.
     * @param uri URI
     */
    public QueryStringDecoder(URI uri) {
        this(uri, StandardCharsets.UTF_8);
    }

    /**
     * Creates a new decoder that decodes the specified URI encoded in the
     * specified charset.
     * @param uri URI
     * @param charset character set
     */
    public QueryStringDecoder(URI uri, Charset charset) {
        this(uri, charset, DEFAULT_MAX_PARAMS);
    }

    /**
     * Creates a new decoder that decodes the specified URI encoded in the
     * specified charset.
     * @param uri URI
     * @param charset character set
     * @param maxParams maximum parameters
     */
    public QueryStringDecoder(URI uri, Charset charset, int maxParams) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        if (maxParams <= 0) {
            throw new IllegalArgumentException(
                    "maxParams: " + maxParams + " (expected: a positive integer)");
        }

        String rawPath = uri.getRawPath();
        if (rawPath != null) {
            hasPath = true;
        } else {
            rawPath = "";
            hasPath = false;
        }
        // Also take care of cut of things like "http://localhost"
        this.uri = rawPath + '?' + uri.getRawQuery();

        this.charset = charset;
        this.maxParams = maxParams;
    }

    /**
     * Returns the uri used to initialize this {@link QueryStringDecoder}.
     * @return uri
     */
    public String uri() {
        return uri;
    }

    /**
     * Returns the decoded path string of the URI.
     * @return path
     */
    public String path() {
        if (path == null) {
            if (!hasPath) {
                return path = "";
            }
            int pathEndPos = uri.indexOf('?');
            if (pathEndPos < 0) {
                path = uri;
            } else {
                return path = uri.substring(0, pathEndPos);
            }
        }
        return path;
    }

    /**
     * Returns the decoded key-value parameter pairs of the URI.
     * @return parameters
     */
    public Map<String, List<String>> parameters() {
        if (params == null) {
            if (hasPath) {
                int pathLength = path().length();
                if (uri.length() == pathLength) {
                    return Collections.emptyMap();
                }
                decodeParams(uri.substring(pathLength + 1));
            } else {
                if (uri.isEmpty()) {
                    return Collections.emptyMap();
                }
                decodeParams(uri);
            }
        }
        return params;
    }

    private void decodeParams(String s) {
        Map<String, List<String>> params = this.params = new LinkedHashMap<String, List<String>>();
        nParams = 0;
        String name = null;
        int pos = 0; // Beginning of the unprocessed region
        int i;       // End of the unprocessed region
        char c;  // Current character
        for (i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            if (c == '=' && name == null) {
                if (pos != i) {
                    name = decodeComponent(s.substring(pos, i), charset);
                }
                pos = i + 1;
                // http://www.w3.org/TR/html401/appendix/notes.html#h-B.2.2
            } else if (c == '&' || c == ';') {
                if (name == null && pos != i) {
                    // We haven't seen an `=' so far but moved forward.
                    // Must be a param of the form '&a&' so add it with
                    // an empty value.
                    if (!addParam(params, decodeComponent(s.substring(pos, i), charset), "")) {
                        return;
                    }
                } else if (name != null) {
                    if (!addParam(params, name, decodeComponent(s.substring(pos, i), charset))) {
                        return;
                    }
                    name = null;
                }
                pos = i + 1;
            }
        }

        if (pos != i) {  // Are there characters we haven't dealt with?
            if (name == null) {     // Yes and we haven't seen any `='.
                addParam(params, decodeComponent(s.substring(pos, i), charset), "");
            } else {                // Yes and this must be the last value.
                addParam(params, name, decodeComponent(s.substring(pos, i), charset));
            }
        } else if (name != null) {  // Have we seen a name without value?
            addParam(params, name, "");
        }
    }

    private boolean addParam(Map<String, List<String>> params, String name, String value) {
        if (nParams >= maxParams) {
            return false;
        }

        List<String> values = params.get(name);
        if (values == null) {
            values = new ArrayList<String>(1);  // Often there's only 1 value.
            params.put(name, values);
        }
        values.add(value);
        nParams++;
        return true;
    }

    /**
     * Decodes a bit of an URL encoded by a browser.
     * This is equivalent to calling {@link #decodeComponent(String, Charset)}
     * with the UTF-8 charset (recommended to comply with RFC 3986, Section 2).
     *
     * @param s The string to decode (can be empty).
     * @return The decoded string, or {@code s} if there's nothing to decode.
     * If the string to decode is {@code null}, returns an empty string.
     * @throws IllegalArgumentException if the string contains a malformed
     *                                  escape sequence.
     */
    public static String decodeComponent(final String s) {
        return decodeComponent(s, StandardCharsets.UTF_8);
    }

    /**
     * Decodes a bit of an URL encoded by a browser.
     * The string is expected to be encoded as per RFC 3986, Section 2.
     * This is the encoding used by JavaScript functions {@code encodeURI}
     * and {@code encodeURIComponent}, but not {@code escape}.  For example
     * in this encoding, &eacute; (in Unicode {@code U+00E9} or in UTF-8
     * {@code 0xC3 0xA9}) is encoded as {@code %C3%A9} or {@code %c3%a9}.
     * This is essentially equivalent to calling
     * {@link URLDecoder#decode(String, String) URLDecoder.decode(s, charset.name())}
     * except that it's over 2x faster and generates less garbage for the GC.
     * Actually this function doesn't allocate any memory if there's nothing
     * to decode, the argument itself is returned.
     *
     * @param s       The string to decode (can be empty).
     * @param charset The charset to use to decode the string (should really
     *                be UTF-8).
     * @return The decoded string, or {@code s} if there's nothing to decode.
     * If the string to decode is {@code null}, returns an empty string.
     * @throws IllegalArgumentException if the string contains a malformed
     *                                  escape sequence.
     */
    public static String decodeComponent(final String s, final Charset charset) {
        if (s == null) {
            return "";
        }
        final int size = s.length();
        boolean modified = false;
        for (int i = 0; i < size; i++) {
            final char c = s.charAt(i);
            if (c == '%' || c == '+') {
                modified = true;
                break;
            }
        }
        if (!modified) {
            return s;
        }
        final byte[] buf = new byte[size];
        int pos = 0;  // position in `buf'.
        for (int i = 0; i < size; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    buf[pos++] = ' ';  // "+" -> " "
                    break;
                case '%':
                    if (i == size - 1) {
                        throw new IllegalArgumentException("unterminated escape"
                                + " sequence at end of string: " + s);
                    }
                    c = s.charAt(++i);
                    if (c == '%') {
                        buf[pos++] = '%';  // "%%" -> "%"
                        break;
                    }
                    if (i == size - 1) {
                        throw new IllegalArgumentException("partial escape"
                                + " sequence at end of string: " + s);
                    }
                    c = decodeHexNibble(c);
                    final char c2 = decodeHexNibble(s.charAt(++i));
                    if (c == Character.MAX_VALUE || c2 == Character.MAX_VALUE) {
                        throw new IllegalArgumentException(
                                "invalid escape sequence `%" + s.charAt(i - 1)
                                        + s.charAt(i) + "' at index " + (i - 2)
                                        + " of: " + s);
                    }
                    c = (char) (c * 16 + c2);
                    // Fall through.
                default:
                    buf[pos++] = (byte) c;
                    break;
            }
        }
        return new String(buf, 0, pos, charset);
    }

    /**
     * Helper to decode half of a hexadecimal number from a string.
     *
     * @param c The ASCII character of the hexadecimal number to decode.
     *          Must be in the range {@code [0-9a-fA-F]}.
     * @return The hexadecimal value represented in the ASCII character
     * given, or {@link Character#MAX_VALUE} if the character is invalid.
     */
    private static char decodeHexNibble(final char c) {
        if ('0' <= c && c <= '9') {
            return (char) (c - '0');
        } else if ('a' <= c && c <= 'f') {
            return (char) (c - 'a' + 10);
        } else if ('A' <= c && c <= 'F') {
            return (char) (c - 'A' + 10);
        } else {
            return Character.MAX_VALUE;
        }
    }
}
