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

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;
import org.jtemplate.ResponseMapping;
import org.jtemplate.sql.Parameters;
import org.jtemplate.sql.ResultSetAdapter;

/**
 * Pet servlet.
 */
@WebServlet(urlPatterns={"/pets/*"}, loadOnStartup=1)
public class PetServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    private static final String DB_URL = "jdbc:mysql://db.local:3306/menagerie?user=root&password=password";

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ServletException(exception);
        }
    }

    /**
     * Retrieves a list of pets belonging to a given owner.
     *
     * @param owner
     * The pet owner to search for.
     *
     * @return
     * A list of pets belonging to the given owner.
     */
    @RequestMethod("GET")
    @ResponseMapping(name="pets~csv.txt", mimeType="text/csv", charset="ISO-8859-1", attachment=true)
    @ResponseMapping(name="pets~html.txt", mimeType="text/html")
    @ResponseMapping(name="pets~xml.txt", mimeType="application/xml")
    public ResultSetAdapter getPets(String owner) throws SQLException {
        Parameters parameters = Parameters.parse("select name, species, sex, birth from pet where owner = :owner");

        PreparedStatement statement = DriverManager.getConnection(DB_URL).prepareStatement(parameters.getSQL());

        parameters.apply(statement, mapOf(entry("owner", owner)));

        return new ResultSetAdapter(statement.executeQuery());
    }
}
