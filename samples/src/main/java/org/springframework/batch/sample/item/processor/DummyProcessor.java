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

package org.springframework.batch.sample.item.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemProcessor;

/**
 * Dummy processor useful for development and testing.
 * 
 * @author Robert Kasanicky
 */
public class DummyProcessor implements ItemProcessor {
	
	private static final Log log = LogFactory.getLog(DummyProcessor.class);
	
    public void process(Object object) {
        log.debug("PROCESSING: " + object);
    }

	public void close() {
	}

	public void init() {
	}
}
