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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jtemplate.beans.BeanAdapter;

/**
 * Event service.
 */
public class EventService {
    /**
     * Adds an event.
     *
     * @param name
     * The name of the event.
     */
    public void addEvent(String title) {
        SessionFactory sessionFactory = HibernateSessionFactoryManager.getSessionFactory();

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
    public List<Map<String, ?>> getEvents() {
        SessionFactory sessionFactory = HibernateSessionFactoryManager.getSessionFactory();

        List<?> events;
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            events = session.createQuery("from Event").list();
            session.getTransaction().commit();
        }

        return BeanAdapter.adapt(events);
    }
}
