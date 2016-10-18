# Introduction
JTemplate is an open-source implementation of the [CTemplate](https://github.com/OlafvdSpek/ctemplate) templating system (aka "Mustache") for Java. It also provides a set of classes for implementing template-driven REST services in Java.

This document introduces the JTemplate framework and provides an overview of its key features. The first section introduces the template syntax used by the CTemplate system. The remaining sections discuss the classes provided by the JTemplate framework for processing templates and implementing REST services.

# Contents
* [Template Syntax](#template-syntax)
* [Implementation](#implementation)
    * [Template Processing](#template-processing)
    * [REST Services](#rest-services)
* [Additional Information](#additional-information)

# Template Syntax
Templates are documents that describe an output format such as HTML, XML, or CSV. They allow the ultimate representation of a data structure to be specified independently of the data itself, promoting a clear separation of responsibility.

The CTemplate system defines a set of "markers" that are replaced with values supplied by the data structure (which CTemplate calls a "data dictionary") when a template is processed. The following CTemplate marker types are supported by JTemplate:

* {{_variable_}} - injects a variable from the data dictionary into the output
* {{#_section_}}...{{/_section_}} - defines a repeating section of content
* {{>_include_}} - imports content specified by another template
* {{!_comment_}} - provides informational text about a template's content

The data dictionary is provided by an instance of `java.util.Map` whose entries represent the values supplied by the dictionary. For example, the contents of the following map might represent the result of some simple statistical calculations:

    {
        "count": 3, 
        "sum": 9.0,
        "average": 3.0
    }

A template for transforming this data into HTML is shown below:

    <html>
    <head>
        <title>Statistics</title>
    </head>
    <body>
        <p>Count: {{count}}</p>
        <p>Sum: {{sum}}</p>
        <p>Average: {{average}}</p> 
    </body>
    </html>

At execution time, the "count", "sum", and "average" markers are replaced by their corresponding values from the data dictionary, producing the following markup:

    <html>
    <head>
        <title>Statistics</title>
    </head>
    <body>
        <p>Count: 3</p>
        <p>Sum: 9.0</p>
        <p>Average: 3.0</p> 
    </body>
    </html>

## Variable Markers
Variable markers inject a value from the data dictionary into the output. For example:

    <p>Count: {{count}}</p>
    <p>Sum: {{sum}}</p>
    <p>Average: {{average}}</p> 

Nested values can be referred to using dot-separated path notation; e.g. "name.first". Missing (i.e. `null`) values are replaced with the empty string in the generated output. 

Non-map values are automatically wrapped in a map instance and assigned a default name of ".". This name can be used to refer to the value in a template. For example, the following variable marker simply echoes a value: 

	The value is {{.}}.

### Resource References
Variable names beginning with "@" represent "resource references". Resources allow static template content to be localized. 

For example, the descriptive text from the statistics template might be extracted into a properties file as follows:

    title=Statistics
    count=Count
    sum=Sum
    average=Average

The template could be updated to refer to the localized values as shown below:

    <html>
    <head>
        <title>{{@title}}</title>
    </head>
    <body>
        <p>{{@count}}: {{count}}</p>
        <p>{{@sum}}: {{sum}}</p>
        <p>{{@average}}: {{average}}</p> 
    </body>
    </html>

When the template is processed, the resource references will be replaced with the corresponding values from the resource bundle.

### Context References
Variable names beginning with "$" represent "context references". Context values can be used to provide additional information to a template that is not included in the data dictionary. 

For example, if the template context contains a value named "currentDate", the following template could be used to inject the date into the output:

    <p>{{$currentDate}}</p>

### Modifiers
The CTemplate specification defines a syntax for applying an optional set of "modifiers" to a variable. Modifiers are used to transform a variable's representation before it is written to the output stream; for example, to apply an escape sequence.

Modifiers are specified as shown below. They are invoked in order from left to right. An optional argument value may be included to provide additional information to the modifier:

    {{variable:modifier1:modifier2:modifier3=argument:...}}

JTemplate provides the following set of standard modifiers:

* `format` - applies a format string
* `^html`, `^xml` - applies markup encoding to a value
* `^json` - applies JSON encoding to a value
* `^csv` - applies CSV encoding to a value
* `^url` - applies URL encoding to a value

For example, the following marker applies a format string to a value and then URL-encodes the result:

    {{value:format=0x%04x:^url}}

In addition to `printf()`-style formatting, the `format` modifier also supports the following arguments:

  * `currency` - applies a locale-specific currency format
  * `percent` - applies a locale-specific percentage format
  * `time` - formats a date as a millisecond value since midnight on January 1, 1970
  * `shortDate` - applies a locale-specific short date format
  * `mediumDate` - applies a locale-specific medium date format
  * `longDate` - applies a locale-specific long date format
  * `fullDate` - applies a locale-specific full date format
  * `isoDate` - formats a local date as an ISO date string
  * `shortTime` - applies a locale-specific short time format
  * `mediumTime` - applies a locale-specific medium time format
  * `longTime` - applies a locale-specific long time format
  * `fullTime` - applies a locale-specific full time format
  * `isoTime` - formats a local time as an ISO time string
  * `shortDateTime` - applies a locale-specific short date/time format
  * `mediumDateTime` - applies a locale-specific medium date/time format
  * `longDateTime` - applies a locale-specific long date/time format
  * `fullDateTime` - applies a locale-specific full date/time format
  * `isoDateTime` - formats a local date/time as an ISO date/time string

For example, this marker transforms a date value into a localized medium-length date string:

    {{date:format=mediumDate}}

Applications may also define their own custom modifiers. This is discussed in more detail later.

## Section Markers
Section markers define a repeating section of content. The marker name must refer to an iterable value in the data dictionary (for example, an instance of `java.util.List`). Content between the markers is repeated once for each element in the list. The elements provide the data dictionary for each successive iteration through the section. If the iterable value is missing (i.e. `null`) or empty, the section's content is excluded from the output.

For example, a data dictionary that contains information about homes for sale might look like this:

    {
        "properties": [
            {
                "streetAddress": "17 Cardinal St.",
                "listPrice": 849000,
                "numberOfBedrooms": 4,
                "numberOfBathrooms": 3
            },
            {
                "streetAddress": "72 Wedgemere Ave.",
                "listPrice": 1650000,
                "numberOfBedrooms": 5,
                "numberOfBathrooms": 3
            },
            ...        
        ]
    }
    
A template to present these results in an HTML table is shown below. The `format` modifier is used to present the list price as a localized currency value:

    <html>
    <head>
        <title>Property Listings</title>
    </head>
    <body>
    <table>
    <tr>
        <td>Street Address</td> 
        <td>List Price</td> 
        <td># Bedrooms</td> 
        <td># Bathrooms</em></td> 
    </tr>
    {{#properties}}
    <tr>
        <td>{{streetAddress}}</td> 
        <td>{{listPrice:format=currency}}</td> 
        <td>{{numberOfBedrooms}}</td> 
        <td>{{numberOfBathrooms}}</td>
    </tr>
    {{/properties}}
    </table>
    </body>
    </html>

Dot notation can also be used with section markers. For example:

    {{#.}}
        ...
    {{/}}

### Separators
Section markers may specify an optional separator string that will be automatically injected between the section's elements. The separator text is enclosed in square brackets immediately following the section name. 

For example, the elements of the "addresses" section specified below will be separated by a comma in the generated output:

    {{#addresses[,]}}
    ...
    {{/addresses}}

## Includes
Include markers import content defined by another template. They can be used to create reusable content modules; for example, document headers and footers. 

For example, the following template, _hello.txt_, includes another document named _world.txt_: 

    Hello, {{>world.txt}}!
    
When _hello.txt_ is processed, the include marker will be replaced with the contents of _world.txt_. For example, if _world.txt_ contains the text "World", the result of processing _hello.txt_ would be the following:

	Hello, World!

Includes inherit their context from the parent document, so they can refer to elements in the parent's data dictionary. This allows includes to be parameterized. Self-referencing includes can also be used to facilitate recursion.

## Comments
Comment markers provide informational text about a template's content. They are not included in the final output. For example, when the following template is processed, only the content between the `<p>` tags will be included:

    {{! Some placeholder text }}
    <p>Lorem ipsum dolor sit amet.</p>

# Implementation
JTemplate is distributed as a JAR file that contains the following classes for processing templates:

* `org.jtemplate`
    * `TemplateEncoder` - template processing engine
    * `Modifier` - interface representing a modifier
* `org.jtemplate.beans`
    * `BeanAdapter` - adapter class that presents the contents of a Java Bean instance as a map
* `org.jtemplate.sql`
    * `ResultSetAdapter` - adapter class that presents the contents of a JDBC result set as an iterable cursor
* `org.jtemplate.util`
    * `IteratorAdapter` - adapter class that presents the contents of an iterator as an iterable cursor

It also includes the following classes for implementing template-driven REST services:

* `org.jtemplate`
    * `DispatcherServlet` - abstract base class for REST services
    * `RequestMethod` - annotation that specifies the HTTP verb associated with a service method
    * `ResponseMapping` - annotation that associates a template with a method result
    * `JSONEncoder` - class used for encoding responses that are not associated with a template
* `org.jtemplate.sql`
    * `Parameters` - class for simplifying execution of prepared statements 

Both sets of classes are discussed in more detail below.

The JTemplate JAR file can be downloaded [here](https://github.com/gk-brown/JTemplate/releases). Java 8 or later is required.

## Template Processing
The `TemplateEncoder` class is responsible for merging a template document with a data dictionary. It provides the following constructors:

    public TemplateEncoder(URL url, String mimeType) { ... }
    public TemplateEncoder(URL url, String mimeType, Charset charset) { ... }
    
The first argument specifies the URL of the template document (generally as a resource on the application's classpath). The second represents the MIME type of the content produced by the template. The third argument represents the optional character encoding used by the template document. The default value is UTF-8.

The following methods can be used to get and set the optional base name of the resource bundle that will be used to resolve resource references:

    public String getBaseName() { ... }
    public void setBaseName(String baseName) { ... }

Values can be added to the template context using the following method, which returns a map representing the context entries:

    public Map<String, Object> getContext() { ... }
    
Templates are applied using one of the following methods:

    public void writeValue(Object value, OutputStream outputStream) { ... }
    public void writeValue(Object value, OutputStream outputStream, Locale locale) { ... }
    public void writeValue(Object value, Writer writer) { ... }
    public void writeValue(Object value, Writer writer, Locale locale) { ... }
    
The first argument represents the value to write (i.e. the data dictionary), and the second the output destination. The optional third argument represents the locale for which the template will be applied. If unspecified, the current default locale is used.

For example, the following code snippet applies a template named _map.txt_ to the contents of a data dictionary whose values are specified by a hash map:

    HashMap<String, Object> map = new HashMap<>();
    
    map.put("a", "hello");
    map.put("b", 123");
    map.put("c", true);

    TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("map.txt"));

    String result;
    try (StringWriter writer = new StringWriter()) {
        encoder.writeValue(map, writer);
        
        result = writer.toString();
    }
    
    System.out.println(result);

If _map.txt_ is specified as follows:

    a = {{a}}, b = {{b}}, c = {{c}}

the example code would produce the following output:

    a = hello, b = 123, c = true
    
### Custom Modifiers 
Modifiers are created by implementing the `Modifier` interface, which defines the following method:

    public Object apply(Object value, String argument, Locale locale);
    
The first argument to this method represents the value to be modified, and the second is the optional argument value following the `=` character in the modifier string. If an argument is not specified, this value will be `null`. The third argument contains the template's locale.

For example, the following class implements a modifier that converts values to uppercase:

    public class UppercaseModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale) {
            return value.toString().toUpperCase(locale);
        }
    }

Custom modifiers are registered by adding them to the modifier map returned by `TemplateEncoder#getModifiers()`. The map key represents the name that is used to apply a modifier in a template document. For example:

	TemplateEncoder.getModifiers().put("uppercase", new UppercaseModifier());

Note that modifiers must be thread-safe, since they are shared and may be invoked concurrently by multiple encoder instances.

### BeanAdapter Class
The `BeanAdapter` class implements the `Map` interface and exposes any properties defined by the Bean as entries in the map, allowing custom data types to be used in a data dictionary.

For example, the following Bean class might be used to represent the simple statistical data discussed earlier:

    public class Statistics {
        private int count = 0;
        private double sum = 0;
        private double average = 0;
    
        public int getCount() {
            return count;
        }
    
        public void setCount(int count) {
            this.count = count;
        }
    
        public double getSum() {
            return sum;
        }
    
        public void setSum(double sum) {
            this.sum = sum;
        }
    
        public double getAverage() {
            return average;
        }
    
        public void setAverage(double average) {
            this.average = average;
        }
    }

Although the values are actually stored in the strongly typed properties of the `Statistics` object, the adapter makes the data appear as a map; for example:

    {
        "count": 3, 
        "sum": 9.0,
        "average": 3.0
    }

An example that uses `BeanAdapter` to apply a template to a `Statistics` instance is shown below:

    Statistics statistics = new Statistics();
    
    statistics.setCount(3);
    statistics.setSum(9.0);
    statistics.setAverage(3.0);

    TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("statistics.txt"));

    String result;
    try (StringWriter writer = new StringWriter()) {
        encoder.writeValue(new BeanAdapter(statistics), writer);
        
        result = writer.toString();
    }
    
    System.out.println(result);

Note that, if a property returns a nested Bean type, the property's value will be automatically wrapped in a `BeanAdapter` instance. Additionally, if a property returns a `List` or `Map` type, the value will be wrapped in an adapter of the appropriate type that automatically adapts its sub-elements.

### ResultSetAdapter Class
The `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing query results to be used as a data dictionary. It also implements `AutoCloseable`: closing the adapter closes the underlying result set, statement, and connection, ensuring that database resources are not leaked. 

For example:

    String sql = "select name, species, sex, birth from pet";

    TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("pets.txt"));

    String result;
    try (StringWriter writer = new StringWriter();
        ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery(sql))) {
        encoder.writeValue(resultSetAdapter, writer);
                
        result = writer.toString();
    }
    
    System.out.println(result);

### IteratorAdapter Class
The `IteratorAdapter` class implements the `Iterable` interface and makes each value produced by an iterator appear to be an element of the adapter, allowing the iterator's contents to be used as a data dictionary. It also implements `AutoCloseable`: if the underlying iterator type is itself an instance of `AutoCloseable`, closing the adapter also closes the underlying cursor.

`IteratorAdapter` is typically used to transform result data produced by NoSQL databases such as MongoDB. It can also be used to transform the result of stream operations on Java collection types. For example:

    Stream<String> stream = Arrays.asList("a", "b", "c").stream();
    
    TemplateEncoder encoder = new TemplateEncoder(getClass().getResource("stream.txt"));

    String result;
    try (StringWriter writer = new StringWriter()) {
        encoder.writeValue(new IteratorAdapter(stream.iterator()), writer);
        
        result = writer.toString();
    }

    System.out.println(result);

## REST Services
In addition to the template processing classes discussed in the previous section, JTemplate provides several classes for use in implementing template-driven REST services. These classes are discussed in more detail below.

### DispatcherServlet Class
`DispatcherServlet` is an abstract base class for REST services. Service operations are defined by adding public methods to a concrete service implementation. 

Services are accessed by submitting an HTTP request for a path associated with a servlet instance. Arguments are provided either via the query string or in the request body, like an HTML form. `DispatcherServlet` converts the request parameters to the expected argument types, invokes the method, and writes the return value to the response stream.

The `RequestMethod` annotation is used to associate a service method with an HTTP verb such as `GET` or `POST`. The optional `ResponseMapping` annotation associates a template document with a method result. If specified, `TemplateEncoder` is used to apply the template to the return value to produce the final response. If no response mapping is specified, the return value is automatically serialized as JSON using the `JSONEncoder` class. `RequestMethod`, `ResponseMapping`, and `JSONEncoder` are all discussed in more detail below.

For example, the following class might be used to implement a service that performs the simple statistical calculations discussed in the previous section:

    @WebServlet(urlPatterns={"/statistics.html"})
    public class StatisticsServlet extends DispatcherServlet {
        @RequestMethod("GET")
        @ResponseMapping("statistics.html")
        public Map<String, ?> getStatistics(List<Double> values) {    
            int count = values.size();

            double sum = 0;
            
            for (int i = 0; i < count; i++) {
                sum += values.get(i);
            }

            double average = sum / count;
                        
            return mapOf(
                entry("count", count),
                entry("sum", sum),
                entry("average", average)
            );
        }
    }

A `GET` for this URL might produce an HTML document similar to the one shown in the first section.

    /statistics.html?values=1&values=3&values=5

#### Method Arguments
Method arguments may be any of the following types:

* `byte`/`Byte`
* `short`/`Short`
* `int`/`Integer`
* `long`/`Long`
* `float`/`Float`
* `double`/`Double`
* `boolean`/`Boolean`
* `String`
* `java.net.URL`
* `java.time.LocalDate`
* `java.time.LocalTime`
* `java.time.LocalDateTime`
* `java.util.Date`
* `java.util.List`

Parameter values for numeric and boolean arguments are converted to the appropriate type using the parse method of the associated wrapper class (e.g. `Integer#parseInt()`). No coercion is necessary for `String` arguments. 

`URL` arguments represent binary content, such as a file upload submitted via an HTML form. As with HTML, they can only be used with `POST` requests submitted using the "multipart/form-data" encoding. Additionally, the servlet must be tagged with the `javax.servlet.annotation.MultipartConfig` annotation; for example:

    @MultipartConfig
    public class FileUploadServlet extends DispatcherServlet {
        @RequestMethod("POST") 
        public void upload(URL file) throws IOException { 
            ... 
        }
    }

Date and time arguments are converted as follows:

* `java.time.LocalDate`: result of calling `LocalDate#parse()` on parameter value
* `java.time.LocalTime`: result of calling `LocalTime#parse()` on parameter value
* `java.time.LocalDateTime`: result of calling `LocalDateTime#parse()` on parameter value
* `java.util.Date`: result of calling `Long#parseLong()` on parameter value, then `Date(long)` on long result

`List` arguments represent multi-value parameters, such as those submitted via a multi-select list element in an HTML form. Values are automatically coerced to the declared `List` element type; e.g. `List<Double>` or `List<String>`. Lists of `URL` values may be used to process multi-file uploads; however, as with single-file uploads, they may only be used with multipart `POST` requests. 

Omitting the value of a primitive parameter results in an argument value of 0 for that parameter. Omitting the value of a simple reference type parameter produces a `null` argument value for that parameter. Omitting all values for a list type parameter produces an empty list argument for the parameter.

Note that service classes must be compiled with the `-parameters` flag so their method parameter names are available at runtime.

#### Return Values
Methods may return any of the following types:

* `byte`/`Byte`
* `short`/`Short`
* `int`/`Integer`
* `long`/`Long`
* `float`/`Float`
* `double`/`Double`
* `boolean`/`Boolean`
* `CharSequence`
* `java.time.LocalDate`
* `java.time.LocalTime`
* `java.time.LocalDateTime`
* `java.util.Date`
* `java.util.Iterable`
* `java.util.Map`

Methods may also return `void` or `Void` to indicate that they do not produce a value.

`Map` implementations must use `String` values for keys. Nested structures are supported, but reference cycles are not permitted.

Return values whose types implement `AutoCloseable` (such as the `ResultSetAdapter` and `IteratorAdapter` classes discussed earlier) will be automatically closed after their contents have been written to the output stream. This allows service implementations to stream response data rather than buffering it in memory before it is written.

If the method completes successfully and returns a value, an HTTP 200 status code is returned. If the method returns `void` or `Void`, HTTP 204 is returned.

If any exception is thrown while executing the method, HTTP 500 is returned. If an exception is thrown while serializing the response, the output is truncated. In either case, the exception is logged.

#### Request and Repsonse Properties
`DispatcherServlet` provides the following methods to allow an implementing class to get access to the current request and response objects; for example, to get the name of the authenticated user or to add a custom header to the response:

    protected HttpServletRequest getRequest() { ... }
    protected HttpServletResponse getResponse() { ... }
    
The methods return thread-local values set by `DispatcherServlet` before a service method is invoked.

### RequestMethod Annotation
The `RequestMethod` annotation is used to associate an HTTP verb with a service method. The method must be publicly accessible. All public annotated methods automatically become available for remote execution when the service is published. 

Multiple methods may be associated with the same verb. `DispatcherServlet` selects the best method to execute based on the names of the provided argument values. For example, a service might define the following methods, both of which are mapped to the `GET` method:

    @RequestMapping("GET")
    public double getSum(double a, double b) {
        return a + b;
    }
    
    @RequestMapping("GET")
    public double getSum(List<Double> values) {
        double total = 0;
    
        for (double value : values) {
            total += value;
        }
    
        return total;
    }

The following request would cause the first method to be invoked:

    GET /math/sum?a=2&b=4
    
This request would invoke the second method:

    GET /math/sum?values=1&values=2&values=3

An HTTP 405 response is returned when no method matching the given arguments can be found.

### ResponseMapping Annotation
The optional `ResponseMapping` annotation is used to associate a template with a service response. The annotation specifies the name of the template that will be applied to the value returned by the method, along with an optional character encoding. The default is UTF-8.

Multiple templates may be associated with a single method. For example, the following service retrieves a list of pets by owner name. Results may be returned either as CSV, HTML, or XML:

    @WebServlet(urlPatterns={
        "/pets/*",
        "/pets.csv",
        "/pets.html",
        "/pets.xml"
    }, loadOnStartup=1)
    public class PetServlet extends DispatcherServlet {
        @RequestMethod("GET")
        @ResponseMapping(name="pets.csv", charset="ISO-8859-1")
        @ResponseMapping(name="pets.html")
        @ResponseMapping(name="pets.xml")
        public ResultSetAdapter getPets(String owner) throws SQLException {
            ...
        }
    }

If no template is associated with a request, the value returned by the method will be encoded as JSON using the `JSONEncoder` class. For example, a `GET` for the following URL would information about all pets belonging to "Gwen" as a JSON document:

    /pets?owner=Gwen

`JSONEncoder` is discussed in more detail in the next section.

Any resource references in a template document are resolved against the resource bundle with the same base name as the service type, using the locale specified by the current HTTP request. Additionally, `DispatcherServlet` provides the following context properties to the template encoder. These values can be used to access request-specific information in a template document:

* `scheme` - the scheme used to make the request; e.g. "http" or "https"
* `serverName` - the host name of the server to which the request was sent
* `serverPort` - the port to which the request was sent
* `contextPath` - the context path of the web application handling the request

For example, the following markup uses the `contextPath` value to embed a product image in an HTML template:

    <img src="{{$contextPath}}/images/{{productID}}.jpg"/>

### JSONEncoder Class 
The `JSONEncoder` class is used to encode service responses that are not associated with a template.

TODO Type handling

### Parameters Class
The `Parameters` class can be used to simplify execution of prepared statements. It provides a means for executing statements using named parameter values rather than indexed arguments. Parameter names are specified by a leading `:` character. For example:

    SELECT * FROM some_table 
    WHERE column_a = :a OR column_b = :b OR column_c = COALESCE(:c, 4.0)
    
The `parse()` method is used to create a `Parameters` instance from a SQL statement. It takes a string or reader containing the SQL text as an argument; for example:

    Parameters parameters = Parameters.parse(sql);

The `getSQL()` method returns the parsed SQL in standard JDBC syntax:

    SELECT * FROM some_table 
    WHERE column_a = ? OR column_b = ? OR column_c = COALESCE(?, 4.0)

This value is used to create the actual prepared statement:

    PreparedStatement statement = DriverManager.getConnection(url).prepareStatement(parameters.getSQL());

Parameter values are specified via a map passed to the `apply()` method:

    parameters.apply(statement, mapOf(entry("a", "hello"), entry("b", 3)));

Once applied, the statement can be executed:

    return new ResultSetAdapter(statement.executeQuery());    

# Additional Information
For additional information and examples, see the [the wiki](https://github.com/gk-brown/JTemplate/wiki).
