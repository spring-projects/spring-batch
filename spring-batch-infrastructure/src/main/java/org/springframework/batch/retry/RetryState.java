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
package org.springframework.batch.retry;

/**
 * @author Dave Syer
 *
 */
public class RetryState {

	final private Object key;
	final private boolean forceRefresh;

	/**
	 * @param key
	 * @param forceRefresh
	 */
	public RetryState(Object key, boolean forceRefresh) {
		this.key = key;
		this.forceRefresh = forceRefresh;
	}

	public RetryState(Object key) {
		this(key, false);
	}
	/**
	 * @return the key that this state represents
	 */
	public Object getKey() {
		return key;
	}

	/**
	 * @return true if the state requires an explicit check for the key
	 */
	public boolean isForceRefresh() {
		return forceRefresh;
	}

}
