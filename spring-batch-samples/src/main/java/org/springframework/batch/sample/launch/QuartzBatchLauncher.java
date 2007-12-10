/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.launch;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class QuartzBatchLauncher {
    private static Log log = LogFactory.getLog(QuartzBatchLauncher.class);

    public static void main(String[] args) throws IOException {
        if (args[0] == null) {
            log.error("Missing argument: provide a path to configuration file");
            System.exit(-1);
        }

        new ClassPathXmlApplicationContext(args[0] + ".xml");

        log.info("Quartz context initialized");
    }
}
