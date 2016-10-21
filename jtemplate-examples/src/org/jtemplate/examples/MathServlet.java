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

import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;
import org.jtemplate.ResourcePath;
import org.jtemplate.ResponseMapping;

/**
 * Math servlet.
 */
@WebServlet(urlPatterns={"/math/*"}, loadOnStartup=1)
public class MathServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    /**
     * Calculates the sum of two values.
     *
     * @param a
     * The first value.
     *
     * @param b
     * The second value.
     *
     * @return
     * The sum of the two values.
     */
    @RequestMethod("GET")
    @ResourcePath("/sum")
    public double getSum(double a, double b) {
        return a + b;
    }

    /**
     * Calculates the sum of a list of values.
     *
     * @param values
     * The list of values.
     *
     * @return
     * The sum of the values in the list.
     */
    @RequestMethod("GET")
    @ResourcePath("/sum")
    public double getSum(List<Double> values) {
        double total = 0;

        for (double value : values) {
            total += value;
        }

        return total;
    }

    /**
     * Calculates simple statistical data from a list of values.
     *
     * @param values
     * The list of values.
     *
     * @return
     * A map containing the result of the calculations.
     */
    @RequestMethod("GET")
    @ResourcePath("/statistics")
    @ResponseMapping(name="statistics~html.txt", mimeType="text/html")
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
