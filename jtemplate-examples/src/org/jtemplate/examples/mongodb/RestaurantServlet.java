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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.bson.Document;
import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;
import org.jtemplate.ResponseMapping;
import org.jtemplate.util.IteratorAdapter;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

/**
 * Restaurant service.
 */
@WebServlet(urlPatterns={"/restaurants/*"}, loadOnStartup=1)
public class RestaurantServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    private MongoClient mongoClient = null;

    @Override
    public void init() throws ServletException {
        super.init();

        mongoClient = new MongoClient("db.local");
    }

    @Override
    public void destroy() {
        if (mongoClient != null) {
            mongoClient.close();
        }

        super.destroy();
    }

    /**
     * Retrieves a list of restaurants in a given zip code.
     *
     * @param zipCode
     * The zip code to search for.
     *
     * @return
     * A list of restaurants in the given zip code.
     */
    @RequestMethod("GET")
    @ResponseMapping(name="restaurants~csv.txt", mimeType="text/csv", charset="ISO-8859-1", attachment=true)
    @ResponseMapping(name="restaurants~html.txt", mimeType="text/html")
    @ResponseMapping(name="restaurants~xml.txt", mimeType="application/xml")
    public IteratorAdapter getRestaurants(String zipCode) {
        MongoDatabase db = mongoClient.getDatabase("test");

        FindIterable<Document> iterable = db.getCollection("restaurants").find(new Document("address.zipcode", zipCode));

        return new IteratorAdapter(iterable.iterator());
    }
}
