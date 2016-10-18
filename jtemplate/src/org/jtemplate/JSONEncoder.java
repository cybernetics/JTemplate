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
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * JSON encoder.
 */
public class JSONEncoder extends Encoder {
    private int depth = 0;

    /**
     * Constructs a new JSON encoder.
     */
    public JSONEncoder() {
        super("application/json", Charset.forName("UTF-8"));
    }

    @Override
    public void writeValue(Object value, Writer writer, Locale locale) throws IOException {
        if (value == null) {
            writer.append(null);
        } else if (value instanceof CharSequence) {
            CharSequence string = (CharSequence)value;

            writer.append("\"");

            for (int i = 0, n = string.length(); i < n; i++) {
                char c = string.charAt(i);

                if (c == '"' || c == '\\') {
                    writer.append("\\" + c);
                } else if (c == '\b') {
                    writer.append("\\b");
                } else if (c == '\f') {
                    writer.append("\\f");
                } else if (c == '\n') {
                    writer.append("\\n");
                } else if (c == '\r') {
                    writer.append("\\r");
                } else if (c == '\t') {
                    writer.append("\\t");
                } else {
                    writer.append(c);
                }
            }

            writer.append("\"");
        } else if (value instanceof Number || value instanceof Boolean) {
            writer.append(String.valueOf(value));
        } else if (value instanceof Date) {
            writeValue(((Date)value).getTime(), writer);
        } else if (value instanceof LocalDate) {
            writeValue(((LocalDate)value).format(DateTimeFormatter.ISO_LOCAL_DATE), writer);
        } else if (value instanceof LocalTime) {
            writeValue(((LocalTime)value).format(DateTimeFormatter.ISO_LOCAL_TIME), writer);
        } else if (value instanceof LocalDateTime) {
            writeValue(((LocalDateTime)value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), writer);
        } else if (value instanceof Iterable<?>) {
            Iterable<?> iterable = (Iterable<?>)value;

            writer.append("[");

            depth++;

            int i = 0;

            for (Object element : iterable) {
                if (i > 0) {
                    writer.append(",");
                }

                writer.append("\n");

                indent(writer);

                writeValue(element, writer);

                i++;
            }

            depth--;

            writer.append("\n");

            indent(writer);

            writer.append("]");
        } else if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>)value;

            writer.append("{");

            depth++;

            int i = 0;

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) {
                    writer.append(",");
                }

                writer.append("\n");

                Object key = entry.getKey();

                if (key == null) {
                    continue;
                }

                indent(writer);

                writeValue(key.toString(), writer);

                writer.append(": ");

                writeValue(entry.getValue(), writer);

                i++;
            }

            depth--;

            writer.append("\n");

            indent(writer);

            writer.append("}");
        } else {
            writeValue(value.toString(), writer);
        }
    }

    private void indent(Writer writer) throws IOException {
        for (int i = 0; i < depth; i++) {
            writer.append("  ");
        }
    }
}
