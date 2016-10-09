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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jtemplate.TemplateEncoder;
import org.jtemplate.beans.BeanAdapter;

/**
 * Event servlet.
 */
public class EventServlet extends HttpServlet {
    private static final long serialVersionUID = 0;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Class<?> type = getClass();
        String servletPath = request.getServletPath();

        response.setContentType(getServletContext().getMimeType(servletPath));

        TemplateEncoder templateEncoder = new TemplateEncoder(type.getResource(servletPath.substring(1)), type.getName());

        SessionFactory sessionFactory = HibernateSessionFactoryManager.getSessionFactory();

        List<?> events;
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            events = session.createQuery("from Event").list();
            session.getTransaction().commit();
        }

        templateEncoder.writeValue(BeanAdapter.adapt(events), response.getOutputStream());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        SessionFactory sessionFactory = HibernateSessionFactoryManager.getSessionFactory();

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.save(new Event(request.getParameter("title"), new Date()));
            session.getTransaction().commit();
        }

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
