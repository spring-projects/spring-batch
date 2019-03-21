/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.partition;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;

/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 */
public class ExampleItemWriter implements ItemWriter<String> {

	private static final Log log = LogFactory.getLog(ExampleItemWriter.class);

	private static List<String> items = new ArrayList<>();

	public static void clear() {
		items.clear();
	}

	public static List<String> getItems() {
		return items;
	}

	/**
	 * @see ItemWriter#write(List)
	 */
	@Override
	public void write(List<? extends String> data) throws Exception {
		log.info(data);
		items.addAll(data);
	}

}
