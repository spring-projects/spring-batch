/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * <p>
 * A {@link ItemWriter} implementation that writes to a MongoDB store using an implementation of Spring Data's
 * {@link MongoOperations}.  Since MongoDB is not a transactional store, a best effort is made to persist
 * written data at the last moment, yet still honor job status contracts.  No attempt to roll back is made
 * if an error occurs during writing.
 * </p>
 *
 * <p>
 * This writer is thread-safe once all properties are set (normal singleton behavior) so it can be used in multiple
 * concurrent transactions.
 * </p>
 *
 * @author Michael Minella
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 *
 */
public class MongoItemWriter<T> implements ItemWriter<T>, InitializingBean {

	private static final String ID_KEY = "_id";
	private MongoOperations template;
	private final Object bufferKey;
	private String collection;
	private boolean delete = false;

	public MongoItemWriter() {
		super();
		this.bufferKey = new Object();
	}

	/**
	 * Indicates if the items being passed to the writer are to be saved or
	 * removed from the data store.  If set to false (default), the items will
	 * be saved.  If set to true, the items will be removed.
	 *
	 * @param delete removal indicator
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * Set the {@link MongoOperations} to be used to save items to be written.
	 *
	 * @param template the template implementation to be used.
	 */
	public void setTemplate(MongoOperations template) {
		this.template = template;
	}

	/**
	 * Get the {@link MongoOperations} to be used to save items to be written.
	 * This can be called by a subclass if necessary.
	 * 
	 * @return template the template implementation to be used.
	 */
	protected MongoOperations getTemplate() {
		return template;
	}

	/**
	 * Set the name of the Mongo collection to be written to.
	 *
	 * @param collection the name of the collection.
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}

	/**
	 * If a transaction is active, buffer items to be written just before commit.
	 * Otherwise write items using the provided template.
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(List)
	 */
	@Override
	public void write(List<? extends T> items) throws Exception {
		if(!transactionActive()) {
			doWrite(items);
			return;
		}

		List<T> bufferedItems = getCurrentBuffer();
		bufferedItems.addAll(items);
	}

	/**
	 * Performs the actual write to the store via the template.
	 * This can be overridden by a subclass if necessary.
	 *
	 * @param items the list of items to be persisted.
	 */
	protected void doWrite(List<? extends T> items) {
		if (!CollectionUtils.isEmpty(items)) {
			if (this.delete) {
				delete(items);
			}
			else {
				saveOrUpdate(items);
			}
		}
	}

	private void delete(List<? extends T> items) {
		BulkOperations bulkOperations = initBulkOperations(BulkMode.ORDERED, items.get(0));
		MongoConverter mongoConverter = this.template.getConverter();
		for (Object item : items) {
			Document document = new Document();
			mongoConverter.write(item, document);
			Object objectId = document.get(ID_KEY);
			if (objectId != null) {
				Query query = new Query().addCriteria(Criteria.where(ID_KEY).is(objectId));
				bulkOperations.remove(query);
			}
		}
		bulkOperations.execute();
	}

	private void saveOrUpdate(List<? extends T> items) {
		BulkOperations bulkOperations = initBulkOperations(BulkMode.ORDERED, items.get(0));
		MongoConverter mongoConverter = this.template.getConverter();
		FindAndReplaceOptions upsert = new FindAndReplaceOptions().upsert();
		for (Object item : items) {
			Document document = new Document();
			mongoConverter.write(item, document);
			Object objectId = document.get(ID_KEY) != null ? document.get(ID_KEY) : new ObjectId();
			Query query = new Query().addCriteria(Criteria.where(ID_KEY).is(objectId));
			bulkOperations.replaceOne(query, document, upsert);
		}
		bulkOperations.execute();
	}

	private BulkOperations initBulkOperations(BulkMode bulkMode, Object item) {
		BulkOperations bulkOperations;
		if (StringUtils.hasText(this.collection)) {
			bulkOperations = this.template.bulkOps(bulkMode, this.collection);
		}
		else {
			bulkOperations = this.template.bulkOps(bulkMode, ClassUtils.getUserClass(item));
		}
		return bulkOperations;
	}

	private boolean transactionActive() {
		return TransactionSynchronizationManager.isActualTransactionActive();
	}

	@SuppressWarnings("unchecked")
	private List<T> getCurrentBuffer() {
		if(!TransactionSynchronizationManager.hasResource(bufferKey)) {
			TransactionSynchronizationManager.bindResource(bufferKey, new ArrayList<T>());

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
				@Override
				public void beforeCommit(boolean readOnly) {
					List<T> items = (List<T>) TransactionSynchronizationManager.getResource(bufferKey);

					if(!CollectionUtils.isEmpty(items)) {
						if(!readOnly) {
							doWrite(items);
						}
					}
				}

				@Override
				public void afterCompletion(int status) {
					if(TransactionSynchronizationManager.hasResource(bufferKey)) {
						TransactionSynchronizationManager.unbindResource(bufferKey);
					}
				}
			});
		}

		return (List<T>) TransactionSynchronizationManager.getResource(bufferKey);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(template != null, "A MongoOperations implementation is required.");
	}
}
