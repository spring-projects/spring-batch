/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.batch.item.data;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 * A {@link ItemWriter} implementation that writes to a Neo4j database using an
 * implementation of Spring Data's {@link Neo4jOperations}.
 * </p>
 *
 * <p>
 * This writer is thread safe once all properties are set (normal singleton
 * behavior) so it can be used in multiple concurrent transactions.
 * </p>
 *
 * @author Michael Minella
 *
 */
public class Neo4jItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory
			.getLog(Neo4jItemWriter.class);

	private boolean delete = false;

	private Neo4jOperations template;

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * Set the {@link Neo4jOperations} to be used to save items
	 *
	 * @param template the template implementation to be used
	 */
	public void setTemplate(Neo4jOperations template) {
		this.template = template;
	}

	/**
	 * Checks mandatory properties
	 *
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(template != null, "A Neo4JOperations implementation is required");
	}

	/**
	 * Write all items to the data store.
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	public void write(List<? extends T> items) throws Exception {
		if(!CollectionUtils.isEmpty(items)) {
			doWrite(items);
		}
	}

	/**
	 * Performs the actual write using the template.  This can be overriden by
	 * a subclass if necessary.
	 *
	 * @param items the list of items to be persisted.
	 */
	protected void doWrite(List<? extends T> items) {
		if(delete) {
			for (T t : items) {
				template.delete(t);
			}
		}
		else {
			for (T t : items) {
				template.save(t);
			}
		}
	}
}
