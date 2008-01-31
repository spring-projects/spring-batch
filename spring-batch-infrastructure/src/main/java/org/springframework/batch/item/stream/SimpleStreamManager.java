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

/**
 * Simple {@link StreamManager} that tries to resolve conflicts between key
 * names by using the short class name of a stream to prefix property keys.
 * 
 * TODO: actually implement the uniqueness strategy!
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
		return new SimpleStreamManagerStreamContext(set);
	}

	/**
	 * @param list a list of {@link ItemStream}s
	 * @return aggregated streamcontext
	 */
	private StreamContext aggregate(Collection list) {
		Properties result = new Properties();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			ItemStream provider = (ItemStream) iterator.next();
			Properties properties = provider.getStreamContext().getProperties();
			if (properties != null) {
				String prefix = ""; // ClassUtils.getShortClassName(provider.getClass()) + ".";
				for (Iterator propiter = properties.keySet().iterator(); propiter.hasNext();) {
					String key = (String) propiter.next();
					String value = properties.getProperty(key);
					result.setProperty(prefix + key, value);
				}
			}
		}
		return new GenericStreamContext(result);
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

	private class SimpleStreamManagerStreamContext implements StreamContext {

		private StreamContext data;

		public SimpleStreamManagerStreamContext(Set streams) {
			this.data = aggregate(streams);
		}

		public Properties getProperties() {
			if (data != null) {
				return data.getProperties();
			}
			return new Properties();
		}
	}

}
