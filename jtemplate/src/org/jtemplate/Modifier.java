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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

/**
 * Interface representing a modifier.
 */
public interface Modifier {
    /**
     * Applies the modifier.
     *
     * @param value
     * The value to which the modifier is being be applied.
     *
     * @param argument
     * The modifier argument, or <tt>null</tt> if no argument was provided.
     *
     * @param locale
     * The locale in which the modifier is being applied.
     *
     * @return
     * The modified value.
     */
    public Object apply(Object value, String argument, Locale locale);
}

// Format modifier
class FormatModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument, Locale locale) {
        Object result;
        if (argument != null) {
            switch (argument) {
                case "currency": {
                    result = NumberFormat.getCurrencyInstance(locale).format(value);

                    break;
                }

                case "percent": {
                    result = NumberFormat.getPercentInstance(locale).format(value);

                    break;
                }

                case "time": {
                    result = ((Date)value).getTime();

                    break;
                }

                case "fullDate": {
                    if (value instanceof LocalDate) {
                        result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
                    } else {
                        result = DateFormat.getDateInstance(DateFormat.FULL, locale).format(value);
                    }

                    break;
                }

                case "longDate": {
                    if (value instanceof LocalDate) {
                        result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale));
                    } else {
                        result = DateFormat.getDateInstance(DateFormat.LONG, locale).format(value);
                    }

                    break;
                }

                case "mediumDate": {
                    if (value instanceof LocalDate) {
                        result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));
                    } else {
                        result = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(value);
                    }

                    break;
                }

                case "shortDate": {
                    if (value instanceof LocalDate) {
                        result = ((LocalDate)value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale));
                    } else {
                        result = DateFormat.getDateInstance(DateFormat.SHORT, locale).format(value);
                    }

                    break;
                }

                case "isoLocalDate": {
                    result = ((LocalDate)value).format(DateTimeFormatter.ISO_LOCAL_DATE);

                    break;
                }

                case "fullTime": {
                    if (value instanceof LocalTime) {
                        result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL).withLocale(locale));
                    } else {
                        result = DateFormat.getTimeInstance(DateFormat.FULL, locale).format(value);
                    }

                    break;
                }

                case "longTime": {
                    if (value instanceof LocalTime) {
                        result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG).withLocale(locale));
                    } else {
                        result = DateFormat.getTimeInstance(DateFormat.LONG, locale).format(value);
                    }

                    break;
                }

                case "mediumTime": {
                    if (value instanceof LocalTime) {
                        result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale));
                    } else {
                        result = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(value);
                    }

                    break;
                }

                case "shortTime": {
                    if (value instanceof LocalTime) {
                        result = ((LocalTime)value).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale));
                    } else {
                        result = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(value);
                    }

                    break;
                }

                case "isoLocalTime": {
                    result = ((LocalTime)value).format(DateTimeFormatter.ISO_LOCAL_TIME);

                    break;
                }

                case "fullDateTime": {
                    if (value instanceof LocalDateTime) {
                        result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(locale));
                    } else {
                        result = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale).format(value);
                    }

                    break;
                }

                case "longDateTime": {
                    if (value instanceof LocalDateTime) {
                        result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withLocale(locale));
                    } else {
                        result = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale).format(value);
                    }

                    break;
                }

                case "mediumDateTime": {
                    if (value instanceof LocalDateTime) {
                        result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale));
                    } else {
                        result = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale).format(value);
                    }

                    break;
                }

                case "shortDateTime": {
                    if (value instanceof LocalDateTime) {
                        result = ((LocalDateTime)value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale));
                    } else {
                        result = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(value);
                    }

                    break;
                }

                case "isoLocalDateTime": {
                    result = ((LocalDateTime)value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    break;
                }

                default: {
                    result = String.format(locale, argument, value);

                    break;
                }
            }
        } else {
            result = value;
        }

        return result;
    }
}

// CSV escape modifier
class CSVEscapeModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument, Locale locale) {
        StringBuilder resultBuilder = new StringBuilder();

        String string = value.toString();

        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);

            if (c == '"') {
                resultBuilder.append(c);
            }

            resultBuilder.append(c);
        }

        return resultBuilder.toString();
    }
}

// JSON escape modifier
class JSONEscapeModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument, Locale locale) {
        StringBuilder resultBuilder = new StringBuilder();

        String string = value.toString();

        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);

            if (c == '"' || c == '\\') {
                resultBuilder.append("\\" + c);
            } else if (c == '\b') {
                resultBuilder.append("\\b");
            } else if (c == '\f') {
                resultBuilder.append("\\f");
            } else if (c == '\n') {
                resultBuilder.append("\\n");
            } else if (c == '\r') {
                resultBuilder.append("\\r");
            } else if (c == '\t') {
                resultBuilder.append("\\t");
            } else {
                resultBuilder.append(c);
            }
        }

        return resultBuilder.toString();
    }
}

// Markup escape modifier
class MarkupEscapeModifier implements Modifier {
    @Override
    public Object apply(Object value, String argument, Locale locale) {
        StringBuilder resultBuilder = new StringBuilder();

        String string = value.toString();

        for (int i = 0, n = string.length(); i < n; i++) {
            char c = string.charAt(i);

            if (c == '<') {
                resultBuilder.append("&lt;");
            } else if (c == '>') {
                resultBuilder.append("&gt;");
            } else if (c == '&') {
                resultBuilder.append("&amp;");
            } else if (c == '"') {
                resultBuilder.append("&quot;");
            } else {
                resultBuilder.append(c);
            }
        }

        return resultBuilder.toString();
    }
}

// URL escape modifier
class URLEscapeModifier implements Modifier {
    private static final String UTF_8_ENCODING = "UTF-8";

    @Override
    public Object apply(Object value, String argument, Locale locale) {
        String result;
        try {
            result = URLEncoder.encode(value.toString(), UTF_8_ENCODING);
        } catch (UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }

        return result;
    }
}

