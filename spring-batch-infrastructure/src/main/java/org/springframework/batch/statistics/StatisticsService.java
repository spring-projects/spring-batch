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

import java.util.Properties;

/**
 * Generalised statistics aggregation strategy. Clients register
 * {@link StatisticsProvider} instances under a well-known key, and then when
 * they ask for statistics by that key, they receive an aggregate of all the
 * values given by registered providers.
 * 
 * @author Dave Syer
 * 
 */
public interface StatisticsService {

	/**
	 * Register the {@link StatisticsProvider} instance as one of possibly
	 * several that are associated with the given key.
	 * 
	 * @param key the key under which to add the provider
	 * @param provider a {@link StatisticsProvider}
	 */
	void register(Object key, StatisticsProvider provider);

	/**
	 * Extract and aggregate the statistics from all providers under this key.
	 * 
	 * @param key the key under which {@link StatisticsProvider} instances might
	 * have been registered.
	 * @return {@link Properties} summarising the statistics of all providers
	 * registered under this key, or empty otherwise.
	 */
	Properties getStatistics(Object key);

}
