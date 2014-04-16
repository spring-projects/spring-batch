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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.batch.api.chunk.AbstractItemWriter;
import java.util.List;

/**
 * <p>
 * Sample {@link javax.batch.api.chunk.ItemWriter} implementation.
 * </p>
 *
 * @since 3.0
 * @author Chris Schaefer
 */
public class JsrSampleItemWriter extends AbstractItemWriter {
	private static final Log LOG = LogFactory.getLog(JsrSampleItemWriter.class);

	@Override
	public void writeItems(List<Object> people) throws Exception {
		for(Object person : people) {
			LOG.info("Writing person: " + person);
		}
	}
}
