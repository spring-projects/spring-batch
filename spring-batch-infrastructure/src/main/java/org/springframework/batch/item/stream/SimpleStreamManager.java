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
package org.springframework.batch.item.stream;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.StreamException;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.support.PropertiesConverter;

/**
 * Simple {@link StreamManager} that makes no attempt to aggregate or resolve
 * conflicts between key names. All the contributions registered are simply
 * polled and added "as is" to the aggregate properties.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleStreamManager implements StreamManager {

	private Map registry = new HashMap();

	/**
	 * Simple aggregate statistics provider for the contributions registered
	 * under the given key.
	 * 
	 * @see org.springframework.batch.statistics.StatisticsService#getStatistics(java.lang.Object)
	 */
	public StreamContext getStreamContext(Object key) {
		Set set = new LinkedHashSet();
		synchronized (registry) {
			Collection collection = (Collection) registry.get(key);
			if (collection != null) {
				set = new LinkedHashSet(collection);
			}
		}
		return new GenericStreamContext(aggregate(set));
	}

	/**
	 * @param list a list of {@link StatisticsProvider}s
	 * @return aggregated statistics
	 */
	private Properties aggregate(Collection list) {
		Properties result = new Properties();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			ItemStream provider = (ItemStream) iterator.next();
			Properties properties = provider.getStreamContext().getProperties();
			if (properties != null) {
				result.putAll(properties);
			}
		}
		return result;
	}

	/**
	 * Register a {@link ItemStream} as one of the interesting providers under
	 * the provided key.
	 * 
	 * @see org.springframework.batch.statistics.StreamManager#register(java.lang.Object,
	 * org.springframework.batch.statistics.StatisticsProvider)
	 */
	public void register(Object key, ItemStream provider) {
		synchronized (registry) {
			Set set = (Set) registry.get(key);
			if (set == null) {
				set = new LinkedHashSet();
				registry.put(key, set);
			}
			set.add(provider);
		}
	}

	/**
	 * Broadcast the call to close from this {@link StreamContext}.
	 * @throws Exception
	 * 
	 * @see StreamManager#restoreFrom(Object, StreamContext)
	 */
	public void close(Object key) throws StreamException {
		Set set = new LinkedHashSet();
		synchronized (registry) {
			Collection collection = (Collection) registry.get(key);
			if (collection != null) {
				set.addAll(collection);
			}
		}
		for (Iterator iterator = set.iterator(); iterator.hasNext();) {
			ItemStream stream = (ItemStream) iterator.next();
			stream.close();
		}
	}
	
	// TODO: integrate this
	private class SimpleStreamManagerStreamContext implements StreamContext {

		private static final String READER_KEY = "DATA_PROVIDER";

		private static final String WRITER_KEY = "DATA_PROCESSOR";

		private StreamContext readerData;

		private StreamContext writerData;

		public SimpleStreamManagerStreamContext(StreamContext providerData, StreamContext writerData) {
			this.readerData = providerData;
			this.writerData = writerData;
		}

		public SimpleStreamManagerStreamContext(Properties data) {
			readerData = new GenericStreamContext(PropertiesConverter
					.stringToProperties(data.getProperty(READER_KEY)));
			writerData = new GenericStreamContext(PropertiesConverter.stringToProperties(data
					.getProperty(WRITER_KEY)));
		}

		public Properties getProperties() {
			Properties props = new Properties();
			if (readerData != null) {
				props.setProperty(READER_KEY, PropertiesConverter.propertiesToString(readerData.getProperties()));
			}
			if (writerData != null) {
				props.setProperty(WRITER_KEY, PropertiesConverter.propertiesToString(writerData.getProperties()));
			}
			return props;
		}
	}
	
}
