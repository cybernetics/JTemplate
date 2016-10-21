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

import java.util.Arrays;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;
import org.jtemplate.ResponseMapping;
import org.jtemplate.beans.BeanAdapter;

/**
 * Tree servlet.
 */
@WebServlet(urlPatterns={"/tree/*"}, loadOnStartup=1)
public class TreeServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    /**
     * Returns a tree structure representing the months of the four seasons.
     *
     * @return
     * A tree structure containing the months of the year grouped by season.
     */
    @RequestMethod("GET")
    @ResponseMapping(name="tree~html.txt", mimeType="text/html")
    public Map<String, ?> getTree() {
        TreeNode root = new TreeNode("Seasons", false);

        TreeNode winter = new TreeNode("Winter", false);
        winter.getChildren().addAll(Arrays.asList(new TreeNode("January"), new TreeNode("February"), new TreeNode("March")));

        root.getChildren().add(winter);

        TreeNode spring = new TreeNode("Spring", false);
        spring.getChildren().addAll(Arrays.asList(new TreeNode("April"), new TreeNode("May"), new TreeNode("June")));

        root.getChildren().add(spring);

        TreeNode summer = new TreeNode("Summer", false);
        summer.getChildren().addAll(Arrays.asList(new TreeNode("July"), new TreeNode("August"), new TreeNode("September")));

        root.getChildren().add(summer);

        TreeNode fall = new TreeNode("Fall", false);
        fall.getChildren().addAll(Arrays.asList(new TreeNode("October"), new TreeNode("November"), new TreeNode("December")));

        root.getChildren().add(fall);

        return new BeanAdapter(root);
    }
}
