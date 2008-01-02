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

package org.springframework.batch.item.reader;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.file.mapping.FieldSetMapper;
import org.springframework.batch.item.ItemReader;

/**
 * An {@link ItemReader} that delivers a list as its item, storing up objects
 * from the injected {@link ItemReader} until they are ready to be packed out
 * as a collection. The {@link ItemReader} should mark the beginning and end of
 * records with the constant values in {@link FieldSetMapper} ({@link AggregateItemReader#BEGIN_RECORD}
 * and {@link AggregateItemReader#END_RECORD}).<br/>
 * 
 * This class is thread safe (it can be used concurrently by multiple threads)
 * as long as the {@link ItemReader} is also thread safe.
 * 
 * @author Dave Syer
 * 
 */
public class AggregateItemReader extends AbstractItemReader {

	private static final Log log = LogFactory
			.getLog(AggregateItemReader.class);

	private ItemReader inputSource;

	/**
	 * Marker for the end of a multi-object record.
	 */
	public static final Object END_RECORD = new Object();

	/**
	 * Marker for the beginning of a multi-object record.
	 */
	public static final Object BEGIN_RECORD = new Object();

	/**
	 * Get the next list of records.
	 * @throws Exception 
	 * 
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	public Object read() throws Exception {
		ResultHolder holder = new ResultHolder();

		while (process(inputSource.read(), holder)) {
			continue;
		}

		if (!holder.exhausted) {
			return holder.records;
		} else {
			return null;
		}
	}

	private boolean process(Object value, ResultHolder holder) {
		// finish processing if we hit the end of file
		if (value == null) {
			log.debug("Exhausted ItemReader");
			holder.exhausted = true;
			return false;
		}

		// start a new collection
		if (value == AggregateItemReader.BEGIN_RECORD) {
			log.debug("Start of new record detected");
			return true;
		}

		// mark we are finished with current collection
		if (value == AggregateItemReader.END_RECORD) {
			log.debug("End of record detected");
			return false;
		}

		// add a simple record to the current collection
		log.debug("Mapping: " + value);
		holder.records.add(value);
		return true;
	}

	/**
	 * Injection setter for {@link ItemReader}.
	 * 
	 * @param inputSource
	 *            an {@link ItemReader}.
	 */
	public void setItemReader(ItemReader inputSource) {
		this.inputSource = inputSource;
	}

	/**
	 * Private class for temporary state management while item is being
	 * collected.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class ResultHolder {
		Collection records = new ArrayList();
		boolean exhausted = false;
	}

}
