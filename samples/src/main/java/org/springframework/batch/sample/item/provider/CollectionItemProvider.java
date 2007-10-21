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

package org.springframework.batch.sample.item.provider;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.item.provider.AbstractItemProvider;

/**
 * An {@link ItemProvider} that delivers a list as its item, storing up objects
 * from the injected {@link InputSource} until they are ready to be packed out
 * as a collection.<br/>
 *
 * This class is thread safe (it can be used concurrently by multiple threads) as
 * long as the {@link InputSource} is also thread safe.
 *
 * @author Dave Syer
 *
 */
public class CollectionItemProvider extends AbstractItemProvider {

	private static final Log log = LogFactory
			.getLog(CollectionItemProvider.class);

	private InputSource inputSource;

	/**
	 * Get the next list of records.
	 *
	 * @see org.springframework.batch.item.ItemProvider#next()
	 */
	public Object next() {
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
			log.debug("Exhausted InputSource");
			holder.exhausted = true;
			return false;
		}

		// start a new collection
		if (value == FieldSetMapper.BEGIN_RECORD) {
			log.debug("Start of new record detected");
			return true;
		}

		// mark we are finished with current collection
		if (value == FieldSetMapper.END_RECORD) {
			log.debug("End of record detected");
			return false;
		}

		// add a simple record to the current collection
		log.debug("Mapping: " + value);
		holder.records.add(value);
		return true;
	}

	/**
	 * Injection setter for {@link InputSource}.
	 * @param inputSource an {@link InputSource}.
	 */
	public void setInputSource(InputSource inputSource) {
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
