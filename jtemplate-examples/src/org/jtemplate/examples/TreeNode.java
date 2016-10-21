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

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a tree node.
 */
public class TreeNode {
    private String name;

    private ArrayList<TreeNode> children = null;

    /**
     * Constructs a new tree node.
     *
     * @param name
     * The name of the node.
     */
    public TreeNode(String name) {
        this(name, true);
    }

    /**
     * Constructs a new tree node.
     *
     * @param name
     * The name of the node.
     *
     * @param leaf
     * <tt>true</tt> if the node is a leaf node; <tt>false</tt>  if it is a
     * branch.
     */
    public TreeNode(String name, boolean leaf) {
        this.name = name;

        if (!leaf) {
            children = new ArrayList<>();
        }
    }

    /**
     * Returns the name of the node.
     *
     * @return
     * The name of the node.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the node's children.
     *
     * @return
     * The node's children.
     */
    public List<TreeNode> getChildren() {
        return children;
    }
}
