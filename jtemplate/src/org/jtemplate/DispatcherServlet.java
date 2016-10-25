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

    // Resource structure
    private static class Resource {
        public final HashMap<String, LinkedList<Method>> handlerMap = new HashMap<>();
        public final HashMap<String, Resource> resources = new HashMap<>();

        @Override
        public String toString() {
            return handlerMap.keySet().toString() + "; " + resources.toString();
        }
    }

    private Resource root = null;

    private ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    private ThreadLocal<HttpServletResponse> response = new ThreadLocal<>();

    private ThreadLocal<List<String>> keys = new ThreadLocal<>();

    private static final String RESPONSE_MAPPING_PREFIX = "~";

    @Override
    public void init() throws ServletException {
        // Populate resource tree
        root = new Resource();

        Method[] methods = getClass().getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];

            RequestMethod requestMethod = method.getAnnotation(RequestMethod.class);

            if (requestMethod != null) {
                Resource resource = root;

                ResourcePath resourcePath = method.getAnnotation(ResourcePath.class);

                if (resourcePath != null) {
                    String[] components = resourcePath.value().split("/");

                    for (int j = 0; j < components.length; j++) {
                        String component = components[j];

                        if (component.length() == 0) {
                            continue;
                        }

                        Resource child = resource.resources.get(component);

                        if (child == null) {
                            child = new Resource();

                            resource.resources.put(component, child);
                        }

                        resource = child;
                    }
                }

                String verb = requestMethod.value().toLowerCase();

                LinkedList<Method> handlerList = resource.handlerMap.get(verb);

                if (handlerList == null) {
                    handlerList = new LinkedList<>();

                    resource.handlerMap.put(verb, handlerList);
                }

                handlerList.add(method);
            }
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Look up handler list
        Resource resource = root;

        String fileName = request.getServletPath();

        if (fileName.isEmpty()) {
            fileName = request.getContextPath();

            if (fileName.isEmpty()) {
                fileName = request.getServerName();
            }
        }

        fileName = fileName.substring(fileName.lastIndexOf('/') + 1);

        String extension = null;

        LinkedList<String> keys = new LinkedList<>();

        String pathInfo = request.getPathInfo();

        if (pathInfo != null) {
            String[] components = pathInfo.split("/");

            for (int i = 0; i < components.length; i++) {
                String component = components[i];

                if (component.length() == 0) {
                    continue;
                }

                if (component.startsWith(RESPONSE_MAPPING_PREFIX)) {
                    extension = component.substring(RESPONSE_MAPPING_PREFIX.length());
                    break;
                }

                Resource child = resource.resources.get(component);

                if (child == null) {
                    child = resource.resources.get("?");

                    if (child == null) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }

                    keys.add(component);
                }

                resource = child;

                fileName = component;
            }
        }

        LinkedList<Method> handlerList = resource.handlerMap.get(request.getMethod().toLowerCase());

        if (handlerList == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        // Set character encoding
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }

        // Look up handler method
        HashMap<String, LinkedList<String>> parameterMap = getParameterMap(request);
        HashMap<String, LinkedList<File>> fileMap = getFileMap(request);

        Method method = getMethod(handlerList, parameterMap, fileMap);

        if (method == null) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        ServletContext servletContext = getServletContext();

        Encoder encoder = null;

        Class<?> returnType = method.getReturnType();

        if (returnType != Void.TYPE && returnType != Void.class) {
            // Determine encoder type
            if (extension != null) {
                // Look up response mapping
                fileName += "." + extension;

                String mimeType = servletContext.getMimeType(fileName);

                if (mimeType != null) {
                    ResponseMapping[] responseMappings = method.getAnnotationsByType(ResponseMapping.class);

                    for (int i = 0; i < responseMappings.length; i++) {
                        ResponseMapping responseMapping = responseMappings[i];

                        if (responseMapping.mimeType().equals(mimeType)) {
                            Class<?> type = getClass();

                            String name = responseMapping.name();

                            URL url = type.getResource(name);

                            if (url != null) {
                                TemplateEncoder templateEncoder = new TemplateEncoder(url, mimeType, Charset.forName(responseMapping.charset()));

                                templateEncoder.setBaseName(type.getName());

                                templateEncoder.getContext().putAll(mapOf(
                                    entry("scheme", request.getScheme()),
                                    entry("serverName", request.getServerName()),
                                    entry("serverPort", request.getServerPort()),
                                    entry("contextPath", request.getContextPath())
                                ));

                                encoder = templateEncoder;

                                if (responseMapping.attachment()) {
                                    response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
                                }

                                break;
                            } else {
                                servletContext.log(String.format("Template \"%s\" not found.", name));
                            }
                        }
                    }
                }

                if (encoder == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
                    return;
                }
            } else {
                // Use default encoder
                encoder = new JSONEncoder();
            }
        }

        // Invoke handler method
        this.request.set(request);
        this.response.set(response);

        this.keys.set(Collections.unmodifiableList(new ArrayList<>(keys)));

        Object result = null;

        try {
            try {
                result = method.invoke(this, getArguments(method, parameterMap, fileMap));
            } catch (Exception exception) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                Throwable cause = exception.getCause();

                if (cause != null) {
                    servletContext.log(String.format("Error executing method %s().", method.getName()), cause);
                }

                return;
            }

            // Write response
            if (encoder == null) {
                if (!response.isCommitted()) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            } else {
                response.setContentType(String.format("%s;charset=%s", encoder.getMimeType(), encoder.getCharset().name()));

                try {
                    encoder.writeValue(result, response.getOutputStream(), request.getLocale());
                } catch (IOException exception) {
                    servletContext.log(String.format("Error writing response for method %s().", method.getName()), exception);
                }
            }
        } finally {
            // Close result
            if (result instanceof AutoCloseable) {
                try {
                    ((AutoCloseable)result).close();
                } catch (Exception exception) {
                    // No-op
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

    private static HashMap<String, LinkedList<String>> getParameterMap(HttpServletRequest request) {
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

        return parameterMap;
    }

    private static HashMap<String, LinkedList<File>> getFileMap(HttpServletRequest request) throws ServletException, IOException {
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

        return fileMap;
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

                argument = Collections.unmodifiableList(list);
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
     * Returns the list of keys parsed from the request path.
     *
     * @return
     * The list of keys parsed from the request path.
     */
    protected List<String> getKeys() {
        return keys.get();
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
