/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jtemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Template encoder.
 */
public class TemplateEncoder extends Encoder {
    // Marker type enumeration
    private enum MarkerType {
        SECTION_START,
        SECTION_END,
        INCLUDE,
        COMMENT,
        VARIABLE
    }

    private URL url;
    private String mimeType;
    private Charset charset;

    private String baseName = null;
    private HashMap<String, Object> context = new HashMap<>();

    private Map<String, Reader> includes = new HashMap<>();
    private LinkedList<Map<String, Reader>> history = new LinkedList<>();

    private static HashMap<String, Modifier> modifiers = new HashMap<>();

    static {
        modifiers.put("format", new FormatModifier());
        modifiers.put("^url", new URLEscapeModifier());
        modifiers.put("^html", new MarkupEscapeModifier());
        modifiers.put("^xml", new MarkupEscapeModifier());
        modifiers.put("^json", new JSONEscapeModifier());
        modifiers.put("^csv", new CSVEscapeModifier());
    }

    private static final int EOF = -1;

    private static final String RESOURCE_PREFIX = "@";
    private static final String CONTEXT_PREFIX = "$";

    /**
     * Constructs a new template encoder.
     *
     * @param url
     * The URL of the template.
     *
     * @param mimeType
     * The MIME type of the content produced by the template.
     */
    public TemplateEncoder(URL url, String mimeType) {
        this(url, mimeType, Charset.forName("UTF-8"));
    }

    /**
     * Constructs a new template encoder.
     *
     * @param url
     * The URL of the template.
     *
     * @param mimeType
     * The MIME type of the content produced by the template.
     *
     * @param charset
     * The character encoding used by the template.
     */
    public TemplateEncoder(URL url, String mimeType, Charset charset) {
        if (url == null) {
            throw new IllegalArgumentException();
        }

        if (mimeType == null) {
            throw new IllegalArgumentException();
        }

        if (charset == null) {
            throw new IllegalArgumentException();
        }

        this.url = url;
        this.mimeType = mimeType;
        this.charset = charset;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    /**
     * Returns the base name of the template's resource bundle.
     *
     * @return
     * The base name of the template's resource bundle, or <tt>null</tt> if no
     * base name has been set.
     */
    public String getBaseName() {
        return baseName;
    }

    /**
     * Sets the base name of the template's resource bundle.
     *
     * @param baseName
     * The base name of the template's resource bundle, or <tt>null</tt> for no
     * base name.
     */
    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    /**
     * Returns the template context.
     *
     * @return
     * The template context.
     */
    public Map<String, Object> getContext() {
        return context;
    }

    @Override
    public void writeValue(Object value, Writer writer, Locale locale) throws IOException {
        if (value != null) {
            try (InputStream inputStream = url.openStream()) {
                Reader reader = new PagedReader(new InputStreamReader(inputStream, getCharset()));

                writeRoot(value, writer, locale, reader);
            }
        }
    }

    private void writeRoot(Object root, Writer writer, Locale locale, Reader reader) throws IOException {
        Map<?, ?> dictionary;
        if (root instanceof Map<?, ?>) {
            dictionary = (Map<?, ?>)root;
        } else {
            dictionary = Collections.singletonMap(".", root);
        }

        int c = reader.read();

        while (c != EOF) {
            if (c == '{') {
                c = reader.read();

                if (c == '{') {
                    c = reader.read();

                    MarkerType markerType;
                    if (c == '#') {
                        markerType = MarkerType.SECTION_START;
                    } else if (c == '/') {
                        markerType = MarkerType.SECTION_END;
                    } else if (c == '>') {
                        markerType = MarkerType.INCLUDE;
                    } else if (c == '!') {
                        markerType = MarkerType.COMMENT;
                    } else {
                        markerType = MarkerType.VARIABLE;
                    }

                    if (markerType != MarkerType.VARIABLE) {
                        c = reader.read();
                    }

                    StringBuilder markerBuilder = new StringBuilder();

                    while (c != '}' && c != EOF) {
                        markerBuilder.append((char)c);

                        c = reader.read();
                    }

                    if (c == EOF) {
                        throw new IOException("Unexpected end of character stream.");
                    }

                    c = reader.read();

                    if (c != '}') {
                        throw new IOException("Improperly terminated marker.");
                    }

                    String marker = markerBuilder.toString();

                    if (marker.length() == 0) {
                        throw new IOException("Invalid marker.");
                    }

                    switch (markerType) {
                        case SECTION_START: {
                            String separator = null;

                            int n = marker.length();

                            if (marker.charAt(n - 1) == ']') {
                                int i = marker.lastIndexOf('[');

                                if (i != -1) {
                                    separator = marker.substring(i + 1, n - 1);

                                    marker = marker.substring(0, i);
                                }
                            }

                            history.push(includes);

                            Object value = dictionary.get(marker);

                            if (value == null) {
                                value = Collections.emptyList();
                            }

                            if (!(value instanceof Iterable<?>)) {
                                throw new IOException("Invalid section element.");
                            }

                            Iterator<?> iterator = ((Iterable<?>)value).iterator();

                            if (iterator.hasNext()) {
                                includes = new HashMap<>();

                                int i = 0;

                                while (iterator.hasNext()) {
                                    Object element = iterator.next();

                                    if (iterator.hasNext()) {
                                        reader.mark(0);
                                    }

                                    if (i > 0 && separator != null) {
                                        writer.append(separator);
                                    }

                                    writeRoot(element, writer, locale, reader);

                                    if (iterator.hasNext()) {
                                        reader.reset();
                                    }

                                    i++;
                                }
                            } else {
                                includes = new AbstractMap<String, Reader>() {
                                    @Override
                                    public Reader get(Object key) {
                                        return new EmptyReader();
                                    }

                                    @Override
                                    public Set<Entry<String, Reader>> entrySet() {
                                        throw new UnsupportedOperationException();
                                    }
                                };

                                writeRoot(Collections.emptyMap(), new NullWriter(), locale, reader);
                            }

                            includes = history.pop();

                            break;
                        }

                        case SECTION_END: {
                            // No-op
                            return;
                        }

                        case INCLUDE: {
                            Reader include = includes.get(marker);

                            if (include == null) {
                                URL url = new URL(this.url, marker);

                                try (InputStream inputStream = url.openStream()) {
                                    include = new PagedReader(new InputStreamReader(inputStream));

                                    writeRoot(dictionary, writer, locale, include);

                                    includes.put(marker, include);
                                }
                            } else {
                                include.reset();

                                writeRoot(dictionary, writer, locale, include);
                            }

                            break;
                        }

                        case COMMENT: {
                            // No-op
                            break;
                        }

                        case VARIABLE: {
                            String[] components = marker.split(":");

                            String key = components[0];

                            Object value;
                            if (key.startsWith(RESOURCE_PREFIX)) {
                                if (baseName != null) {
                                    ResourceBundle resourceBundle = ResourceBundle.getBundle(baseName, locale);

                                    value = resourceBundle.getString(key.substring(RESOURCE_PREFIX.length()));
                                } else {
                                    value = null;
                                }
                            } else if (key.startsWith(CONTEXT_PREFIX)) {
                                value = context.get(key.substring(CONTEXT_PREFIX.length()));
                            } else if (key.equals(".")) {
                                value = dictionary.get(key);
                            } else {
                                value = dictionary;

                                String[] path = key.split("\\.");

                                for (int i = 0; i < path.length; i++) {
                                    if (!(value instanceof Map<?, ?>)) {
                                        throw new IOException("Invalid path.");
                                    }

                                    value = ((Map<?, ?>)value).get(path[i]);

                                    if (value == null) {
                                        break;
                                    }
                                }
                            }

                            if (value != null) {
                                if (components.length > 1) {
                                    for (int i = 1; i < components.length; i++) {
                                        String component = components[i];

                                        int j = component.indexOf('=');

                                        String name, argument;
                                        if (j == -1) {
                                            name = component;
                                            argument = null;
                                        } else {
                                            name = component.substring(0, j);
                                            argument = component.substring(j + 1);
                                        }

                                        Modifier modifier = modifiers.get(name);

                                        if (modifier != null) {
                                            value = modifier.apply(value, argument, locale);
                                        }
                                    }
                                }

                                writer.append(value.toString());
                            }

                            break;
                        }

                        default: {
                            throw new UnsupportedOperationException();
                        }
                    }
                } else {
                    writer.append('{');
                    writer.append((char)c);
                }
            } else {
                writer.append((char)c);
            }

            c = reader.read();
        }
    }

    /**
     * Returns the modifier map.
     *
     * @return
     * The modifier map.
     */
    public static Map<String, Modifier> getModifiers() {
        return modifiers;
    }
}

// Paged reader
class PagedReader extends Reader {
    private Reader reader;
    private int pageSize;

    private int position = 0;
    private int count = 0;

    private boolean endOfFile = false;

    private ArrayList<char[]> pages = new ArrayList<>();
    private LinkedList<Integer> marks = new LinkedList<>();

    private static int DEFAULT_PAGE_SIZE = 1024;
    private static int EOF = -1;

    public PagedReader(Reader reader) {
        this(reader, DEFAULT_PAGE_SIZE);
    }

    public PagedReader(Reader reader, int pageSize) {
        if (reader == null) {
            throw new IllegalArgumentException();
        }

        this.reader = reader;
        this.pageSize = pageSize;
    }

    @Override
    public int read() throws IOException {
        int c;
        if (position < count) {
            c = pages.get(position / pageSize)[position % pageSize];

            position++;
        } else if (!endOfFile) {
            c = reader.read();

            if (c != EOF) {
                if (position / pageSize == pages.size()) {
                    pages.add(new char[pageSize]);
                }

                pages.get(pages.size() - 1)[position % pageSize] = (char)c;

                position++;
                count++;
            } else {
                endOfFile = true;
            }
        } else {
            c = EOF;
        }

        return c;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int c = 0;
        int n = 0;

        for (int i = off; i < cbuf.length && n < len; i++) {
            c = read();

            if (c == EOF) {
                break;
            }

            cbuf[i] = (char)c;

            n++;
        }

        return (c == EOF && n == 0) ? EOF : n;
    }

    @Override
    public boolean ready() throws IOException {
        return (position < count) || reader.ready();
    }

    @Override
    public void mark(int readAheadLimit) {
        marks.push(position);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void reset() {
        if (marks.isEmpty()) {
            position = 0;
        } else {
            position = marks.pop();
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}

// Empty reader
class EmptyReader extends Reader {
    @Override
    public int read(char cbuf[], int off, int len) {
        return -1;
    }

    @Override
    public void reset() {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }
}

// Null writer
class NullWriter extends Writer {
    @Override
    public void write(char[] cbuf, int off, int len) {
        // No-op
    }

    @Override
    public void flush() {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }
}
