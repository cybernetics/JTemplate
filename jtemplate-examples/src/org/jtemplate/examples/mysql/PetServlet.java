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

package org.jtemplate.examples.mysql;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jtemplate.TemplateEncoder;
import org.jtemplate.sql.ResultSetAdapter;

/**
 * Pet servlet.
 */
@WebServlet(urlPatterns={
    "/pets.csv",
    "/pets.html",
    "/pets.json",
    "/pets.xml"
}, loadOnStartup=1)
@MultipartConfig
public class PetServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    private static final String DB_URL = "jdbc:mysql://db.local:3306/menagerie?user=root&password=password";

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Class<?> type = getClass();
        String servletPath = request.getServletPath();

        response.setContentType(getServletContext().getMimeType(servletPath) + ";charset=UTF-8");

        TemplateEncoder templateEncoder = new TemplateEncoder(type.getResource(servletPath.substring(1)), type.getName());

        String sql = "select name, species, sex, birth from pet where owner = ?";

        try {
            PreparedStatement statement = DriverManager.getConnection(DB_URL).prepareStatement(sql);

            statement.setString(1, request.getParameter("owner"));

            try (ResultSetAdapter resultSetAdapter = new ResultSetAdapter(statement.executeQuery())){
                templateEncoder.writeValue(resultSetAdapter, response.getOutputStream());
            }
        } catch (SQLException exception) {
            throw new ServletException(exception);
        }
    }
}
