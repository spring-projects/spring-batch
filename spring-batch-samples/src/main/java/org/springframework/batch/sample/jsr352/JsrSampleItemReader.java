/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.sample.jsr352;

import java.util.ArrayList;
import java.util.List;
import javax.batch.api.chunk.AbstractItemReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Sample {@link javax.batch.api.chunk.ItemReader} implementation.
 * </p>
 *
 * @since 3.0
 * @author Chris Schaefer
 */
@SuppressWarnings("serial")
public class JsrSampleItemReader extends AbstractItemReader {
	private static final Log LOG = LogFactory.getLog(JsrSampleItemReader.class);

	private List<String> people = new ArrayList<String>() {{
		add("John");
		add("Joe");
		add("Mark");
		add("Jane");
	}};

	@Override
	public Object readItem() throws Exception {
		String person = null;

		if(people.iterator().hasNext()) {
			person = people.iterator().next();
			people.remove(person);

			LOG.info("Read person: " + person);
		}

		return person;
	}
}
