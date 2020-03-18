/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * <p>
 * A {@link ItemWriter} implementation that writes to a Neo4j database.
 * </p>
 *
 * <p>
 * This writer is thread-safe once all properties are set (normal singleton
 * behavior) so it can be used in multiple concurrent transactions.
 * </p>
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 *
 */
public class Neo4jItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory
			.getLog(Neo4jItemWriter.class);

	private boolean delete = false;

	private SessionFactory sessionFactory;

	/**
	 * Boolean flag indicating whether the writer should save or delete the item at write
	 * time.
	 * @param delete true if write should delete item, false if item should be saved.
	 * Default is false.
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * Establish the session factory that will be used to create {@link Session} instances
	 * for interacting with Neo4j.
	 * @param sessionFactory sessionFactory to be used.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Checks mandatory properties
	 *
	 * @see InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(this.sessionFactory != null,
				"A SessionFactory is required");
	}

	/**
	 * Write all items to the data store.
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends T> items) throws Exception {
		if(!CollectionUtils.isEmpty(items)) {
			doWrite(items);
		}
	}

	/**
	 * Performs the actual write using the template.  This can be overridden by
	 * a subclass if necessary.
	 *
	 * @param items the list of items to be persisted.
	 */
	protected void doWrite(List<? extends T> items) {
		if(delete) {
			delete(items);
		}
		else {
			save(items);
		}
	}

	private void delete(List<? extends T> items) {
		Session session = this.sessionFactory.openSession();

		for(T item : items) {
			session.delete(item);
		}
	}

	private void save(List<? extends T> items) {
		Session session = this.sessionFactory.openSession();

		for (T item : items) {
			session.save(item);
		}
	}
}
