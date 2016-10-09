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

package org.jtemplate.examples.mongodb;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;
import org.jtemplate.TemplateEncoder;
import org.jtemplate.util.IteratorAdapter;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/**
 * Restaurant service.
 */
public class RestaurantServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Class<?> type = getClass();
        String servletPath = request.getServletPath();

        response.setContentType(getServletContext().getMimeType(servletPath) + ";charset=UTF-8");

        TemplateEncoder templateEncoder = new TemplateEncoder(type.getResource(servletPath.substring(1)), type.getName());

        MongoDatabase db = MongoClientManager.getMongoClient().getDatabase("test");

        FindIterable<Document> iterable = db.getCollection("restaurants").find(new Document("address.zipcode", request.getParameter("zipCode")));

        try (MongoCursor<Document> cursor = iterable.iterator()) {
            templateEncoder.writeValue(new IteratorAdapter(cursor), response.getOutputStream());
        }
    }
}
