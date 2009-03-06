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

package org.springframework.batch.core.launch;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Mock {@link ItemWriter} that will throw an exception when a certain number of
 * items have been written.
 */
public class EmptyItemWriter<T> implements ItemWriter<T>, InitializingBean {

	private boolean failed = false;

	// point at which to fail...
	private int failurePoint = Integer.MAX_VALUE;

	protected Log logger = LogFactory.getLog(EmptyItemWriter.class);

	List<Object> list;

	public void afterPropertiesSet() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList();
	}

	public void setFailurePoint(int failurePoint) {
		this.failurePoint = failurePoint;
	}

	public void write(List<? extends T> items) {
		for (T data : items) {
			if (!failed && list.size() == failurePoint) {
				failed = true;
				throw new RuntimeException("Failed processing: [" + data + "]");
			}
			logger.info("Processing: [" + data + "]");
			list.add(data);
		}
	}

	public List<Object> getList() {
		return list;
	}

}
