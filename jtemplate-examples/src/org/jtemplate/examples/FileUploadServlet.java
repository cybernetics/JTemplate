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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;

import org.jtemplate.DispatcherServlet;
import org.jtemplate.RequestMethod;

/**
 * File upload servlet.
 */
@WebServlet(urlPatterns={"/upload/*",}, loadOnStartup=1)
@MultipartConfig
public class FileUploadServlet extends DispatcherServlet {
    private static final long serialVersionUID = 0;

    /**
     * Uploads a file.
     *
     * @param file
     * The URL of the file being uploaded.
     *
     * @return
     * The size of the uploaded file, in bytes.
     */
    @RequestMethod("POST")
    public long upload(URL file) throws IOException {
        long bytes = 0;

        try (InputStream inputStream = file.openStream()) {
            while (inputStream.read() != -1) {
                bytes++;
            }
        }

        return bytes;
    }
}
