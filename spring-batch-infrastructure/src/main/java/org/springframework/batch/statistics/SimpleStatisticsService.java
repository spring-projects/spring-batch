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
package org.springframework.batch.statistics;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Simple {@link StatisticsService} that makes no attempt to aggregate or
 * resolve conflicts between key names. All the contributions registered are
 * simply polled and added "as is" to the aggregate properties.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleStatisticsService implements StatisticsService {

	private Map registry = new HashMap();

	/**
	 * Simple aggregate statistics provider for the contributions registered
	 * under the given key.
	 * 
	 * @see org.springframework.batch.statistics.StatisticsService#getStatistics(java.lang.Object)
	 */
	public Properties getStatistics(Object key) {
		Set set = new LinkedHashSet();
		synchronized (registry) {
			Collection collection = (Collection) registry.get(key);
			if (collection != null) {
				set = new LinkedHashSet(collection);
			}
		}
		return aggregate(set);
	}

	/**
	 * @param list a list of {@link StatisticsProvider}s
	 * @return aggregated statistics
	 */
	private Properties aggregate(Collection list) {
		Properties result = new Properties();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			StatisticsProvider provider = (StatisticsProvider) iterator.next();
			Properties properties = provider.getStatistics();
			if (properties != null) {
				result.putAll(properties);
			}
		}
		return result;
	}

	/**
	 * Register a {@link StatisticsProvider} as one of the interesting providers
	 * under the provided key.
	 * 
	 * @see org.springframework.batch.statistics.StatisticsService#register(java.lang.Object,
	 * org.springframework.batch.statistics.StatisticsProvider)
	 */
	public void register(Object key, StatisticsProvider provider) {
		synchronized (registry) {
			Set set = (Set) registry.get(key);
			if (set == null) {
				set = new LinkedHashSet();
				registry.put(key, set);
			}
			set.add(provider);
		}
	}

}
