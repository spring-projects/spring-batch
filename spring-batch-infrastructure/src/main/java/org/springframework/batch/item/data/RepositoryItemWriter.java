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
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;

/**
 * <p>
 * A {@link org.springframework.batch.item.ItemReader} wrapper for a
 * {@link org.springframework.data.repository.CrudRepository} from Spring Data.
 * </p>
 *
 * <p>
 * It depends on {@link org.springframework.data.repository.CrudRepository#save(Iterable)}
 * method to store the items for the chunk.  Performance will be determined by that
 * implementation more than this writer.
 * </p>
 *
 * <p>
 * As long as the repository provided is thread-safe, this writer is also thread-safe once
 * properties are set (normal singleton behavior), so it can be used in multiple concurrent
 * transactions.
 * </p>
 *
 * @author Michael Minella
 * @since 2.2
 */
@SuppressWarnings("rawtypes")
public class RepositoryItemWriter implements ItemWriter, InitializingBean {

	protected static final Log logger = LogFactory.getLog(RepositoryItemWriter.class);

	private CrudRepository repository;

	/**
	 * Set the {@link org.springframework.data.repository.CrudRepository} implementation
	 * for persistence
	 *
	 * @param repository the Spring Data repository to be set
	 */
	public void setRepository(CrudRepository repository) {
		this.repository = repository;
	}

	/**
	 * Write all items to the data store via a Spring Data repository.
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	public void write(List items) throws Exception {
		if(items != null && !items.isEmpty()) {
			doWrite(items);
		}
	}

	/**
	 * Performs the actual write to the repository.  This can be overriden by
	 * a subclass if necessary.
	 *
	 * @param items the list of items to be persisted.
	 */
	@SuppressWarnings("unchecked")
	protected void doWrite(List items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Writing to the repository with " + items.size() + " items.");
		}

		repository.save(items);
	}

	/**
	 * Check mandatory properties - there must be a repository.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(repository, "A CRUDRepository is required");
	}
}
