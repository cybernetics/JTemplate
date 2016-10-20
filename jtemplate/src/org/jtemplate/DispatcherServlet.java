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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Abstract base class for dispatcher servlets.
 */
public abstract class DispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    private HashMap<String, LinkedList<Method>> handlerMap = new HashMap<>();

    private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    @Override
    public void init() throws ServletException {
        // Populate handler map
        Method[] methods = getClass().getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            RequestMethod requestMethod = method.getAnnotation(RequestMethod.class);

            if (requestMethod != null) {
                String key = requestMethod.value().toLowerCase();

                LinkedList<Method> handlerList = handlerMap.get(key);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    handlerMap.put(key, handlerList);
                }

                handlerList.add(method);
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Look up handler list
        LinkedList<Method> handlerList = handlerMap.get(request.getMethod().toLowerCase());

        if (handlerList == null) {
            super.service(request, response);
            return;
        }

        // Set character encoding
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        // Populate parameter map
        HashMap<String, LinkedList<String>> parameterMap = new HashMap<>();

        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] values = request.getParameterValues(name);

            LinkedList<String> valueList = new LinkedList<>();

            for (int i = 0; i < values.length; i++) {
                valueList.add(values[i]);
            }

            parameterMap.put(name, valueList);
        }

        // Populate file map
        HashMap<String, LinkedList<File>> fileMap = new HashMap<>();

        String contentType = request.getContentType();

        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            for (Part part : request.getParts()) {
                String submittedFileName = part.getSubmittedFileName();

                if (submittedFileName == null || submittedFileName.length() == 0) {
                    continue;
                }

                String name = part.getName();

                LinkedList<File> fileList = fileMap.get(name);

                if (fileList == null) {
                    fileList = new LinkedList<>();
                    fileMap.put(name, fileList);
                }

                File file = File.createTempFile(part.getName(), "_" + part.getSubmittedFileName());
                part.write(file.getAbsolutePath());

                fileList.add(file);
            }
        }

        // Look up handler method
        Class<?> type = getClass();
        String typeName = type.getName();

        Method method = getMethod(handlerList, parameterMap, fileMap);

        if (method == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        ServletContext servletContext = getServletContext();

        Encoder encoder = null;

        Class<?> returnType = method.getReturnType();

        if (returnType != Void.TYPE && returnType != Void.class) {
            String name = request.getServletPath().substring(1);

            ResponseMapping[] responseMappings = method.getAnnotationsByType(ResponseMapping.class);

            for (int i = 0; i < responseMappings.length; i++) {
                ResponseMapping responseMapping = responseMappings[i];

                if (responseMapping.name().equals(name)) {
                    URL url = type.getResource(name);

                    if (url == null) {
                        throw new ServletException("Template not found.");
                    }

                    String mimeType = servletContext.getMimeType(name);

                    if (mimeType == null) {
                        mimeType = "text/plain";
                    }

                    TemplateEncoder templateEncoder = new TemplateEncoder(url, mimeType, Charset.forName(responseMapping.charset()));

                    templateEncoder.setBaseName(typeName);

                    templateEncoder.getContext().putAll(mapOf(
                        entry("scheme", request.getScheme()),
                        entry("serverName", request.getServerName()),
                        entry("serverPort", request.getServerPort()),
                        entry("contextPath", request.getContextPath())
                    ));

                    encoder = templateEncoder;

                    break;
                }
            }

            if (encoder == null) {
                encoder = new JSONEncoder();
            }
        }

        // Invoke handler method
        this.request.set(request);
        this.response.set(response);

        Object result = null;

        try {
            try {
                result = method.invoke(this, getArguments(method, parameterMap, fileMap));
            } catch (Exception exception) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                Throwable cause = exception.getCause();

                if (cause != null) {
                    servletContext.log(typeName, cause);
                }

                return;
            }

            // Write response
            if (encoder == null) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setContentType(String.format("%s;charset=%s", encoder.getMimeType(), encoder.getCharset().name()));

                try {
                    encoder.writeValue(result, response.getOutputStream(), request.getLocale());
                } catch (IOException exception) {
                    servletContext.log(typeName, exception);
                }
            }
        } finally {
            // Close result
            if (result instanceof AutoCloseable) {
                try {
                    ((AutoCloseable)result).close();
                } catch (Exception exception) {
                    servletContext.log(typeName, exception);
                }
            }

            // Delete files
            for (LinkedList<File> fileList : fileMap.values()) {
                for (File file : fileList) {
                    file.delete();
                }
            }
        }
    }

    private static Method getMethod(LinkedList<Method> handlerList, HashMap<String, LinkedList<String>> parameterMap,
        HashMap<String, LinkedList<File>> fileMap) {
        Method method = null;

        int n = parameterMap.size() + fileMap.size();

        int i = Integer.MAX_VALUE;

        for (Method handler : handlerList) {
            Parameter[] parameters = handler.getParameters();

            if (parameters.length >= n) {
                int j = 0;

                for (int k = 0; k < parameters.length; k++) {
                    String name = parameters[k].getName();

                    if (!(parameterMap.containsKey(name) || fileMap.containsKey(name))) {
                        j++;
                    }
                }

                if (parameters.length - j == n && j < i) {
                    method = handler;

                    i = j;
                }
            }
        }

        return method;
    }

    private static Object[] getArguments(Method method, HashMap<String, LinkedList<String>> parameterMap,
        HashMap<String, LinkedList<File>> fileMap) throws IOException {
        Parameter[] parameters = method.getParameters();

        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];

            String name = parameter.getName();
            Class<?> type = parameter.getType();

            Object argument;
            if (type == List.class) {
                ParameterizedType parameterizedType = (ParameterizedType)parameter.getParameterizedType();
                Type elementType = parameterizedType.getActualTypeArguments()[0];

                List<Object> list;
                if (elementType == URL.class) {
                    LinkedList<File> fileList = fileMap.get(name);

                    if (fileList != null) {
                        list = new ArrayList<>(fileList.size());

                        for (File file : fileList) {
                            list.add(file.toURI().toURL());
                        }
                    } else {
                        list = Collections.emptyList();
                    }
                } else {
                    LinkedList<String> valueList = parameterMap.get(name);

                    if (valueList != null) {
                        int n = valueList.size();

                        list = new ArrayList<>(n);

                        for (String value : valueList) {
                            list.add(getArgument(value, elementType));
                        }
                    } else {
                        list = Collections.emptyList();
                    }
                }

                argument = list;
            } else if (type == URL.class) {
                LinkedList<File> fileList = fileMap.get(name);

                if (fileList != null) {
                    argument = fileList.getFirst().toURI().toURL();
                } else {
                    argument = null;
                }
            } else {
                LinkedList<String> valueList = parameterMap.get(name);

                String value;
                if (valueList != null) {
                    value = valueList.getFirst();
                } else {
                    value = null;
                }

                argument = getArgument(value, type);
            }

            arguments[i] = argument;
        }

        return arguments;
    }

    private static Object getArgument(String value, Type type) {
        Object argument;
        if (type == String.class) {
            argument = value;
        } else if (type == Byte.TYPE) {
            argument = (value == null) ? 0 : Byte.parseByte(value);
        } else if (type == Byte.class) {
            argument = (value == null) ? null : Byte.parseByte(value);
        } else if (type == Short.TYPE) {
            argument = (value == null) ? 0 : Short.parseShort(value);
        } else if (type == Short.class) {
            argument = (value == null) ? null : Short.parseShort(value);
        } else if (type == Integer.TYPE) {
            argument = (value == null) ? 0 : Integer.parseInt(value);
        } else if (type == Integer.class) {
            argument = (value == null) ? null : Integer.parseInt(value);
        } else if (type == Long.TYPE) {
            argument = (value == null) ? 0 : Long.parseLong(value);
        } else if (type == Long.class) {
            argument = (value == null) ? null : Long.parseLong(value);
        } else if (type == Float.TYPE) {
            argument = (value == null) ? 0 : Float.parseFloat(value);
        } else if (type == Float.class) {
            argument = (value == null) ? null : Float.parseFloat(value);
        } else if (type == Double.TYPE) {
            argument = (value == null) ? 0 : Double.parseDouble(value);
        } else if (type == Double.class) {
            argument = (value == null) ? null : Double.parseDouble(value);
        } else if (type == Boolean.TYPE) {
            argument = (value == null) ? false : Boolean.parseBoolean(value);
        } else if (type == Boolean.class) {
            argument = (value == null) ? null : Boolean.parseBoolean(value);
        } else if (type == Date.class) {
            argument = new Date(Long.parseLong(value));
        } else if (type == LocalDate.class) {
            argument = LocalDate.parse(value);
        } else if (type == LocalTime.class) {
            argument = LocalTime.parse(value);
        } else if (type == LocalDateTime.class) {
            argument = LocalDateTime.parse(value);
        } else {
            throw new UnsupportedOperationException("Invalid parameter type.");
        }

        return argument;
    }

    /**
     * Returns the servlet request.
     *
     * @return
     * The servlet request.
     */
    protected HttpServletRequest getRequest() {
        return request.get();
    }

    /**
     * Returns the servlet response.
     *
     * @return
     * The servlet response.
     */
    protected HttpServletResponse getResponse() {
        return response.get();
    }

    /**
     * Creates a list from a variable length array of elements.
     *
     * @param elements
     * The elements from which the list will be created.
     *
     * @return
     * An immutable list containing the given elements.
     */
    @SafeVarargs
    public static List<?> listOf(Object...elements) {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

    /**
     * Creates a map from a variable length array of map entries.
     *
     * @param <K> The type of the key.
     *
     * @param entries
     * The entries from which the map will be created.
     *
     * @return
     * An immutable map containing the given entries.
     */
    @SafeVarargs
    public static <K> Map<K, ?> mapOf(Map.Entry<K, ?>... entries) {
        HashMap<K, Object> map = new HashMap<>();

        for (Map.Entry<K, ?> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates a map entry.
     *
     * @param <K> The type of the key.
     *
     * @param key
     * The entry's key.
     *
     * @param value
     * The entry's value.
     *
     * @return
     * An immutable map entry containing the key/value pair.
     */
    public static <K> Map.Entry<K, ?> entry(K key, Object value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
