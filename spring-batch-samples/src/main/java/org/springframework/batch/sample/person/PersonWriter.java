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

package org.springframework.batch.sample.person;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.support.AbstractItemWriter;



public class PersonWriter extends AbstractItemWriter<Person> {
    private static Log log = LogFactory.getLog(PersonWriter.class);
 
    public void write(Person data) {
        if (!(data instanceof Person)) {
            log.warn("PersonProcessor can process only Person objects, skipping record");

            return;
        }

        log.debug("Processing: " + data);
    }

}
