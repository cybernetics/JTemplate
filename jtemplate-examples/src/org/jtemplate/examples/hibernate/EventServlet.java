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

package org.jtemplate.examples.hibernate;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;
import org.jtemplate.ResponseMapping;
import org.jtemplate.beans.BeanAdapter;

/**
 * Event servlet.
 */
@WebServlet(urlPatterns={"/events/*"}, loadOnStartup=1)
public class EventServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    private SessionFactory sessionFactory = null;

    @Override
    public void init() throws ServletException {
        super.init();

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();

        sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
    }

    @Override
    public void destroy() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }

        super.destroy();
    }

    /**
     * Adds an event.
     *
     * @param title
     * The title of the event.
     */
    @RequestMethod("POST")
    public void addEvent(String title) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.save(new Event(title, new Date()));
            session.getTransaction().commit();
        }
    }

    /**
     * Retrieves a list of all events.
     *
     * @return
     * A list of all events.
     */
    @RequestMethod("GET")
    @ResponseMapping(name="events~csv.txt", mimeType="text/csv", charset="ISO-8859-1", attachment=true)
    @ResponseMapping(name="events~html.txt", mimeType="text/html")
    @ResponseMapping(name="events~xml.txt", mimeType="application/xml")
    public List<Map<String, ?>> getEvents() {
        List<?> events;
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            events = session.createQuery("from Event").list();

            session.getTransaction().commit();
        }

        return BeanAdapter.adapt(events);
    }
}
