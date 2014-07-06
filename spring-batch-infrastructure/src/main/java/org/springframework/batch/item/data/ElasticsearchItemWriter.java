/*
 * Copyright 2002-2014 the original author or authors.
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

import static java.lang.String.valueOf;
import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.transaction.support.TransactionSynchronizationManager.bindResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.getResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.hasResource;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;
import static org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource;
import static org.springframework.util.Assert.state;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;

import org.slf4j.Logger;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * <p>
 * A {@link ItemWriter} implementation that writes to Elasticsearch store using an implementation of Spring Data's
 * {@link ElasticsearchOperations}.  Similar to MongoDB, Elasticsearch is not a transactional store,
 * the strategy for writing data is similar to {@link MongoItemWriter}. There is no roll back if an error occurs
 * </p>
 *
 * <p>
 * This writer is thread-safe once all properties are set (normal singleton behavior) so it can be used in multiple
 * concurrent transactions.
 * </p>
 *
 * @author Hasnain Javed
 * @since  3.1.0
 *
 */
public class ElasticsearchItemWriter implements ItemWriter<IndexQuery>, InitializingBean {
	
	private final String dataKey;
	private final Logger logger;
	private ElasticsearchOperations elasticsearchOperations;	
	private boolean delete;
	
	public ElasticsearchItemWriter(ElasticsearchOperations elasticsearchOperations) {
		super();
		dataKey = valueOf(randomUUID());
		logger = getLogger(getClass());
		delete = false;
		this.elasticsearchOperations = elasticsearchOperations;
	}
	
	/**
	 * A flag for removing items given to the writer. Default value is set to false indicating that the items will be saved.
	 * otherwise, the items will be removed.
	 *
	 * @param delete flag
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		state(elasticsearchOperations != null, "An ElasticsearchOperations implementation is required.");
	}
	
	@Override
	public void write(List<? extends IndexQuery> items) throws Exception {
		
		if(isActualTransactionActive()) {
			addToBuffer(items);
		}else {
			writeItems(items);
		}
	}
	
	/**
	 * Writes to Elasticsearch via the template.
	 * This can be overridden by a subclass if required.
	 *
	 * @param items the list of items to be indexed.
	 */
	protected void writeItems(List<? extends IndexQuery> items) {
		
		if(isEmpty(items)) {
			logger.warn("no items to write to elasticsearch. list is empty or null");
		}else {
			
			for(IndexQuery item : items) {
				
				if(delete) {
					String id = item.getId();
					logger.debug("deleting item with id {}", id);
					elasticsearchOperations.delete(item.getObject().getClass(), id);
				}else {
					logger.debug("adding item to elasticsearch");
					elasticsearchOperations.index(item);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void addToBuffer(List<? extends IndexQuery> items) {
		
		if(hasResource(dataKey)) {
			logger.debug("appending items to buffer under key {}", dataKey);
			List<IndexQuery> buffer = (List<IndexQuery>) getResource(dataKey);
			buffer.addAll(items);
		}else {
			logger.debug("adding items to buffer under key {}", dataKey);
			bindResource(dataKey, items);
			registerSynchronization(new TransactionSynchronizationCallbackImpl());
		}
	}
	
	private class TransactionSynchronizationCallbackImpl extends TransactionSynchronizationAdapter {
		@SuppressWarnings("unchecked")
		@Override
		public void beforeCommit(boolean readOnly) {
			
			List<IndexQuery> items = (List<IndexQuery>) getResource(dataKey);
			if(!isEmpty(items)) {
				if(!readOnly) {
					writeItems(items);
				}else{
					logger.warn("can not write items to elasticsearch as transaction is read only");
				}
			}else {
				logger.warn("no items to write to elasticsearch. list is empty or null");
			}
		}
		
		@Override
		public void afterCompletion(int status) {
			if(hasResource(dataKey)) {
				logger.debug("removing items from buffer under key {}", dataKey);
				unbindResource(dataKey);
			}
		}
	}
}