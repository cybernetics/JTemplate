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

package org.jtemplate.examples;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;

/**
 * Servlet that echoes a value to the servlet output stream.
 */
@WebServlet(urlPatterns={"/echo/*"}, loadOnStartup=1)
public class EchoServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    /**
     * Writes a value to the servlet output stream.
     *
     * @param value
     * The value to write.
     *
     * @throws IOException
     * If an error occurs while writing the value.
     */
    @RequestMethod("GET")
    public void writeValue(String value) throws IOException {
        HttpServletResponse response = getResponse();

        response.setContentType("text/plain");

        PrintWriter writer = response.getWriter();

        writer.append(value);
        writer.flush();
    }
}
