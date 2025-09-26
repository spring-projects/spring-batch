/*
 * Copyright 2012-2025 the original author or authors.
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

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.BulkOperations.BulkMode;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * <p>
 * A {@link ItemWriter} implementation that writes to a MongoDB store using an
 * implementation of Spring Data's {@link MongoOperations}. Since MongoDB is not a
 * transactional store, a best effort is made to persist written data at the last moment,
 * yet still honor job status contracts. No attempt to roll back is made if an error
 * occurs during writing.
 * </p>
 *
 * <p>
 * This writer is thread-safe once all properties are set (normal singleton behavior) so
 * it can be used in multiple concurrent transactions.
 * </p>
 *
 * @author Michael Minella
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 *
 */
public class MongoItemWriter<T> implements ItemWriter<T>, InitializingBean {

	/**
	 * Operation mode of the item writer.
	 *
	 * @since 5.1
	 */
	public enum Mode {

		/**
		 * Insert items into the target collection using
		 * {@link BulkOperations#insert(Object)}.
		 */
		INSERT,
		/**
		 * Insert or update items into the target collection using
		 * {@link BulkOperations#replaceOne(Query, Object, FindAndReplaceOptions)}.
		 */
		UPSERT,
		/**
		 * Remove items from the target collection using
		 * {@link BulkOperations#remove(Query)}.
		 */
		REMOVE;

	}

	private static final String ID_KEY = "_id";

	private MongoOperations template;

	private final Object bufferKey;

	private String collection;

	private Mode mode = Mode.UPSERT;

	private List<String> primaryKeys = List.of(ID_KEY);

	public MongoItemWriter() {
		super();
		this.bufferKey = new Object();
	}

	/**
	 * Set the operating {@link Mode} to be applied by this writer. Defaults to
	 * {@link Mode#UPSERT}.
	 * @param mode the mode to be used.
	 * @since 5.1
	 */
	public void setMode(final Mode mode) {
		this.mode = mode;
	}

	/**
	 * Get the operating {@link Mode} of the item writer.
	 * @return the operating mode
	 * @since 5.1
	 */
	public Mode getMode() {
		return mode;
	}

	/**
	 * Set the {@link MongoOperations} to be used to save items to be written.
	 * @param template the template implementation to be used.
	 */
	public void setTemplate(MongoOperations template) {
		this.template = template;
	}

	/**
	 * Get the {@link MongoOperations} to be used to save items to be written. This can be
	 * called by a subclass if necessary.
	 * @return template the template implementation to be used.
	 */
	protected MongoOperations getTemplate() {
		return template;
	}

	/**
	 * Set the name of the Mongo collection to be written to.
	 * @param collection the name of the collection.
	 */
	public void setCollection(String collection) {
		this.collection = collection;
	}

	/**
	 * Get the Mongo collection name.
	 * @return the collection name
	 * @since 5.1
	 */
	public String getCollection() {
		return collection;
	}

	/**
	 * Set the primary keys to associate with the document being written. These fields
	 * should uniquely identify a single object.
	 * @param primaryKeys The primary keys to use.
	 * @since 5.2.3
	 */
	public void setPrimaryKeys(List<String> primaryKeys) {
		Assert.notEmpty(primaryKeys, "The primaryKeys list must have one or more keys.");

		this.primaryKeys = primaryKeys;
	}

	/**
	 * Get the list of primary keys associated with the document being written.
	 * @return the list of primary keys
	 * @since 5.2.3
	 */
	public List<String> getPrimaryKeys() {
		return primaryKeys;
	}

	/**
	 * If a transaction is active, buffer items to be written just before commit.
	 * Otherwise write items using the provided template.
	 *
	 * @see org.springframework.batch.item.ItemWriter#write(Chunk)
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {
		if (!transactionActive()) {
			doWrite(chunk);
			return;
		}

		Chunk bufferedItems = getCurrentBuffer();
		bufferedItems.addAll(chunk.getItems());
	}

	/**
	 * Performs the actual write to the store via the template. This can be overridden by
	 * a subclass if necessary.
	 * @param chunk the chunk of items to be persisted.
	 */
	protected void doWrite(Chunk<? extends T> chunk) {
		if (!chunk.isEmpty()) {
			switch (this.mode) {
				case INSERT -> insert(chunk);
				case REMOVE -> remove(chunk);
				default -> upsert(chunk);
			}
		}
	}

	private void insert(final Chunk<? extends T> chunk) {
		final BulkOperations bulkOperations = initBulkOperations(BulkMode.ORDERED, chunk.getItems().get(0));
		final MongoConverter mongoConverter = this.template.getConverter();
		for (final Object item : chunk) {
			final Document document = new Document();
			mongoConverter.write(item, document);
			bulkOperations.insert(document);
		}
		bulkOperations.execute();
	}

	private void remove(Chunk<? extends T> chunk) {
		BulkOperations bulkOperations = initBulkOperations(BulkMode.ORDERED, chunk.getItems().get(0));
		MongoConverter mongoConverter = this.template.getConverter();
		for (Object item : chunk) {
			Document document = new Document();
			mongoConverter.write(item, document);

			List<Criteria> criteriaList = primaryKeys.stream()
				.filter(document::containsKey)
				.map(key -> Criteria.where(key).is(document.get(key)))
				.collect(toList());
			if (!criteriaList.isEmpty()) {
				Query query = new Query();
				criteriaList.forEach(query::addCriteria);
				bulkOperations.remove(query);
			}
		}
		bulkOperations.execute();
	}

	private void upsert(Chunk<? extends T> chunk) {
		BulkOperations bulkOperations = initBulkOperations(BulkMode.ORDERED, chunk.getItems().get(0));
		MongoConverter mongoConverter = this.template.getConverter();
		FindAndReplaceOptions upsert = new FindAndReplaceOptions().upsert();
		for (Object item : chunk) {
			Document document = new Document();
			mongoConverter.write(item, document);

			Query query = new Query();
			List<Criteria> criteriaList = primaryKeys.stream()
				.filter(document::containsKey)
				.map(key -> Criteria.where(key).is(document.get(key)))
				.collect(toList());

			if (criteriaList.isEmpty()) {
				Object objectId = document.get(ID_KEY) != null ? document.get(ID_KEY) : new ObjectId();
				query.addCriteria(Criteria.where(ID_KEY).is(objectId));
			}
			else {
				criteriaList.forEach(query::addCriteria);
			}

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
	@Nullable
	private Chunk<T> getCurrentBuffer() {
		if (!TransactionSynchronizationManager.hasResource(bufferKey)) {
			TransactionSynchronizationManager.bindResource(bufferKey, new Chunk<T>());

			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void beforeCommit(boolean readOnly) {
					Chunk<T> chunk = (Chunk<T>) TransactionSynchronizationManager.getResource(bufferKey);

					if (!chunk.isEmpty()) {
						if (!readOnly) {
							doWrite(chunk);
						}
					}
				}

				@Override
				public void afterCompletion(int status) {
					if (TransactionSynchronizationManager.hasResource(bufferKey)) {
						TransactionSynchronizationManager.unbindResource(bufferKey);
					}
				}
			});
		}

		return (Chunk<T>) TransactionSynchronizationManager.getResource(bufferKey);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(template != null, "A MongoOperations implementation is required.");
	}

}
