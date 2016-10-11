# Introduction
JTemplate is an open-source implementation of the [CTemplate](https://github.com/OlafvdSpek/ctemplate) templating system in Java.

# Contents
* [Template Syntax](#template-overview)
* [Implementation](#implementation)
* [Additional Information](#additional-information)

## Template Syntax
Templates are a means of separating data from presentation. They describe an output format such as HTML, XML, or CSV, and allow the ultimate representation of data structure to be specified independently of the data itself, promoting a clear separation of responsibility.

The CTemplate system defines a set of "markers" that are replaced with values supplied by a data structure (which CTemplate calls a "data dictionary") when a template is processed. The following marker types are supported by JTemplate:

* {{_variable_}} - injects a variable from the data dictionary into the output
* {{#_section_}}...{{/_section_}} - defines a repeating section of content
* {{>_include_}} - imports content specified by another template
* {{!_comment_}} - provides informational text about a template's content

The data dictionary is usually represented by an instance of `java.util.Map` whose keys represent the values provided by the dictionary. For example, the following map values might represent a set of simple statistical values:

    {
        "average": 3.0, 
        "count": 3, 
        "sum": 9.0
    }

A simple template for transforming this data into HTML is shown below:

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
        <p>Count: 3.0</p>
        <p>Sum: 9.0</p>
        <p>Average: 3.0</p> 
    </body>
    </html>

### Variable Markers
Variable markers inject a variable from the data dictionary into the output. For example:

    <p>Count: {{count}}</p>
    <p>Sum: {{sum}}</p>
    <p>Average: {{average}}</p> 

Nested values can be referred to using dot-separated path notation; e.g. "name.first". Missing (i.e. `null`) values are replaced with the empty string in the generated output. 

#### Dot Notation
Although maps are often used to provide a template's data dictionary, this is not strictly required. Non-map values are automatically wrapped in a map instance and assigned a default name of ".". This name can be used to refer to the value in a template. For example, the following variable marker simply echoes a value: 

	The value is {{.}}.

#### Resource References
Variable names beginning with the `@` character represent "resource references". Resources allow static template content to be localized. For example, the descriptive text from the statistics template might be localized as follows:

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

#### Context References
Variable names beginning with the `$` character represent "context references". They can be used to provide additional information to a template that is not included in the data dictionary. For example, if the template context contains a value named "currentDate", the following markup could be used to inject the date into the template output:

    <p>{{$currentDate}}</p>

#### Modifiers
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

  * `currency` - applies a currency format
  * `percent` - applies a percent format
  * `time` - formats a date as a millisecond value since midnight on January 1, 1970
  * `shortDate` - applies a short date format
  * `mediumDate` - applies a medium date format
  * `longDate` - applies a long date format
  * `fullDate` - applies a full date format
  * `isoDate` - formats a local date as an ISO date string
  * `shortTime` - applies a short time format
  * `mediumTime` - applies a medium time format
  * `longTime` - applies a long time format
  * `fullTime` - applies a full time format
  * `isoTime` - formats a local time as an ISO time string
  * `shortDateTime` - applies a short date/time format
  * `mediumDateTime` - applies a medium date/time format
  * `longDateTime` - applies a long date/time format
  * `fullDateTime` - applies a full date/time format
  * `isoDateTime` - formats a local date/time as an ISO date/time string

For example, this marker applies a medium date format to a date value named "date":

    {{date:format=mediumDate}}

Applications may also define their own custom modifiers. This is discussed in more detail later.

### Section Markers
Section markers define a repeating section of content. The marker name must refer to an iterable value in the data dictionary. Content between the markers is repeated once for each element in the collection. The element provides the data dictionary for each successive iteration through the section. If the iterable value is missing (i.e. `null`) or empty, the section's content is excluded from the output.

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

#### Dot Notation
Dot notation can also be used with section markers. For example:

    {{#.}}
    ...
    {{/}}

#### Separators
Section markers may specify an optional separator string that will be automatically injected between the output for each element. The separator text is enclosed in square brackets immediately following the section name. For example, the elements of the "addresses" section below will be separated by a comma in the generated output:

    {{#addresses[,]}}
    ...
    {{/addresses}}

### Includes
Include markers import content defined by another template. They can be used to create reusable content modules; for example, document headers and footers.

For example, the following template, _hello.txt_, includes another document named _world.txt_: 

    Hello, {{>world.txt}}!
    
When _hello.txt_ is processed, the include marker will be replaced with the contents of _world.txt_. For example, if _world.txt_ contains the text "World", the result of processing _hello.txt_ would be the following:

	Hello, World!

Includes inherit their context from the parent document, so they can refer to elements in the parent's data dictionary. This allows includes to be parameterized.

Includes can also be used to facilitate recursion. For example, an include that includes itself could be used to transform the contents of a hierarchical data structure.

### Comments
Comment markers provide informational text about a template's content. They are not included in the final output. For example, when the following template is processed, only the content between the `<p>` tags will be included:

    {{! Some placeholder text }}
    <p>Lorem ipsum dolor sit amet.</p>

## Implementation
JTemplate is distributed as a JAR file that contains the following types, discussed in more detail below:

* `org.jtemplate`
    * `TemplateEncoder` - class for processing template documents
    * `Modifier` - interface representing a template modifier
* `org.jtemplate.beans`
    * `BeanAdapter` - adapter class that presents the contents of a Java Bean instance as a map
* `org.jtemplate.sql`
    * `ResultSetAdapter` - adapter class that presents the contents of a JDBC result set as an iterable cursor
* `org.jtemplate.util`
    * `IteratorAdapter` - adapter class that presents the contents of an iterator as an iterable cursor

The JAR file can be downloaded [here](https://github.com/gk-brown/JTemplate/releases). Java 8 or later is required.

### TemplateEncoder Class
The `TemplateEncoder` class is responsible for merging a template document with a data dictionary. 

TODO

### Custom Modifiers 
Modifiers are created by implementing the `Modifier` interface, which defines the following method:

    public Object apply(Object value, String argument, Locale locale);
    
The first argument to this method represents the value to be modified, and the second is the optional argument value following the `=` character in the modifier string. If a modifier argument is not specified, the value of `argument` will be `null`. The third argument contains the caller's locale.

For example, the following class implements a modifier that converts values to uppercase:

    public class UppercaseModifier implements Modifier {
        @Override
        public Object apply(Object value, String argument, Locale locale) {
            return value.toString().toUpperCase(locale);
        }
    }

Custom modifiers are registered by adding them to the modifier map returned by `TemplateEncoder#getModifiers()`. The map key represents the name that is used to apply a modifier in a template document. For example:

	TemplateEncoder.getModifiers().put("uppercase", new UppercaseModifier());

Note that modifiers must be thread-safe, since they are shared and may be invoked concurrently by multiple template engines.

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
        "average": 3.0, 
        "count": 3, 
        "sum": 9.0
    }

TODO 

Using this class, an implementation of a `getStatistics()` method might look like this:

    Statistics statistics = new Statistics();

    int n = values.size();

    statistics.setCount(n);

    for (int i = 0; i < n; i++) {
        statistics.setSum(statistics.getSum() + values.get(i));
    }

    statistics.setAverage(statistics.getSum() / n);

    return new BeanAdapter(statistics);

Note that, if a property returns a nested Bean type, the property's value will be automatically wrapped in a `BeanAdapter` instance. Additionally, if a property returns a `List` or `Map` type, the value will be wrapped in an adapter of the appropriate type that automatically adapts its sub-elements. This allows service methods to return recursive structures such as trees.

### ResultSetAdapter Class
The `ResultSetAdapter` class implements the `Iterable` interface and makes each row in a JDBC result set appear as an instance of `Map`, allowing the query results to be used in a data dictionary. For example:

TODO

#### Nested Structures
If a column's label contains a period, the value will be returned as a nested structure. For example, the following query might be used to retrieve a list of employee records:

    SELECT first_name AS 'name.first', last_name AS 'name.last', title FROM employees
    
Because the aliases for the `first_name` and `last_name` columns contain a period, each row will contain a nested "name" structure instead of a flat collection of key/value pairs; for example:

    [
      {
        "name": {
          "first": "John",
          "last": "Smith"
        },
        "title": "Manager"
      },
      ...
    ]

### IteratorAdapter Class
The `IteratorAdapter` class implements the `Iterable` interface and makes each item produced by a source iterator appear to be an element of the adapter, allowing the iterator's contents to be used in a data dictionary.

`IteratorAdapter` is typically used to transform result data produced by NoSQL databases such as MongoDB. For example:

TODO

`IteratorAdapter` can also be used to transform the result of stream operations on Java collection types. For example:

TODO

    return new IteratorAdapter(listOf("a", "b", "c").stream().iterator());

# Additional Information
For additional information and examples, see the [the wiki](https://github.com/gk-brown/JTemplate/wiki).
