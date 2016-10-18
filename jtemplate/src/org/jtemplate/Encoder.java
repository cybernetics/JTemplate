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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Abstract base class for encoders.
 */
public abstract class Encoder {
    private String mimeType;
    private Charset charset;

    /**
     * Constructs a new encoder.
     *
     * @param mimeType
     * The encoder's MIME type.
     *
     * @param charset
     * The character encoding.
     */
    public Encoder(String mimeType, Charset charset) {
        if (mimeType == null) {
            throw new IllegalArgumentException();
        }

        if (charset == null) {
            throw new IllegalArgumentException();
        }

        this.mimeType = mimeType;
        this.charset = charset;
    }

    /**
     * Returns the encoder's MIME type.
     *
     * @return
     * The encoder's MIME type.
     */
    public String getMIMEType() {
        return mimeType;
    }

    /**
     * Returns the character encoding.
     *
     * @return
     * The character encoding.
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Writes a value to an output stream.
     *
     * @param value
     * The value to encode.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, OutputStream outputStream) throws IOException {
        writeValue(value, outputStream, Locale.getDefault());
    }

    /**
     * Writes a value to an output stream.
     *
     * @param value
     * The value to encode.
     *
     * @param outputStream
     * The output stream to write to.
     *
     * @param locale
     * The locale to use when writing the value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, OutputStream outputStream, Locale locale) throws IOException {
        Writer writer = new OutputStreamWriter(outputStream, charset);
        writeValue(value, writer, locale);

        writer.flush();
    }

    /**
     * Writes a value to a character stream.
     *
     * @param value
     * The value to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public void writeValue(Object value, Writer writer) throws IOException {
        writeValue(value, writer, Locale.getDefault());
    }

    /**
     * Writes a value to a character stream.
     *
     * @param value
     * The value to encode.
     *
     * @param writer
     * The character stream to write to.
     *
     * @param locale
     * The locale to use when writing the value.
     *
     * @throws IOException
     * If an exception occurs.
     */
    public abstract void writeValue(Object value, Writer writer, Locale locale) throws IOException;
}
