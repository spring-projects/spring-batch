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

package org.springframework.batch.execution.launch;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Mock {@link ItemWriter} that will throw an exception when a certain
 * number of items have been written. 
 */
public class EmptyItemWriter implements ItemWriter, InitializingBean {

	private boolean failed = false;

	// point at which to fail...
	private int failurePoint = Integer.MAX_VALUE;

	protected Log logger = LogFactory.getLog(EmptyItemWriter.class);

	List list;

	public void afterPropertiesSet() throws Exception {
		TransactionAwareProxyFactory factory = new TransactionAwareProxyFactory(new ArrayList());
		list = (List) factory.createInstance();
	}

	public void setFailurePoint(int failurePoint) {
		this.failurePoint = failurePoint;
	}

	public void write(Object data) {
		if (!failed && list.size() == failurePoint) {
			failed = true;
			throw new RuntimeException("Failed processing: [" + data + "]");
		}
		logger.info("Processing: [" + data + "]");
		list.add(data);
	}

	public List getList() {
		return list;
	}

	public void clear() throws Exception {
		//no-op
	}

	public void flush() throws Exception {
		//no-op
	}

}
