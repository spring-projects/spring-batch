/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.batch.core.repository.dao.mongodb;

import java.util.Objects;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;
import org.jspecify.annotations.Nullable;

import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

// Based on https://www.mongodb.com/blog/post/generating-globally-unique-identifiers-for-use-with-mongodb
// Section: Use a single counter document to generate unique identifiers one at a time

/**
 * @author Mahmoud Ben Hassine
 * @author Christoph Strobl
 * @author Yanming Zhou
 * @since 5.2.0
 */
public class MongoSequenceIncrementer implements DataFieldMaxValueIncrementer {

	/*
	 * Retry template to handle errors when incrementing the sequence value
	 * https://github.com/spring-projects/spring-batch/issues/4960
	 *
	 * Typically, only transient errors are retried, and even if
	 * DataIntegrityViolationException extends NonTransientDataAccessException, the
	 * MongoDB driver suggests to retry the operation on write conflict:
	 * "Please retry your operation or multi-document transaction"
	 */
	private final RetryTemplate retryTemplate = new RetryTemplate(
			RetryPolicy.builder().includes(DataIntegrityViolationException.class).build());

	private final MongoOperations mongoTemplate;

	private final String sequenceName;

	/*
	 * Transaction template used to increment the sequence outside of any ongoing
	 * transaction, when a transaction manager is provided.
	 * https://github.com/spring-projects/spring-batch/issues/5507
	 */
	private final @Nullable TransactionTemplate transactionTemplate;

	public MongoSequenceIncrementer(MongoOperations mongoTemplate, String sequenceName) {
		this.mongoTemplate = mongoTemplate;
		this.sequenceName = sequenceName;
		this.transactionTemplate = null;
	}

	/**
	 * Create a new {@link MongoSequenceIncrementer} that increments the sequence outside
	 * of any ongoing transaction.
	 * <p>
	 * The increment runs with {@link TransactionDefinition#PROPAGATION_NOT_SUPPORTED},
	 * suspending whatever transaction may be active on the calling thread for the
	 * duration of the call. As a result, the sequence value is not rolled back if that
	 * transaction later fails for an unrelated reason; the value is simply consumed and
	 * skipped, which is the same behavior as other sequence generators (for example,
	 * database sequences).
	 * @param mongoTemplate the {@link MongoOperations} to use
	 * @param sequenceName the name of the sequence to increment
	 * @param transactionManager the transaction manager used to suspend any ongoing
	 * transaction while incrementing the sequence
	 * @since 6.0.6
	 */
	public MongoSequenceIncrementer(MongoOperations mongoTemplate, String sequenceName,
			PlatformTransactionManager transactionManager) {
		this.mongoTemplate = mongoTemplate;
		this.sequenceName = sequenceName;
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		this.transactionTemplate = template;
	}

	@Override
	public long nextLongValue() throws DataAccessException {
		if (this.transactionTemplate != null) {
			return Objects.requireNonNull(this.transactionTemplate.execute(status -> incrementSequence()),
					"The transaction callback must return a value");
		}
		return incrementSequence();
	}

	private long incrementSequence() throws DataAccessException {
		try {
			return retryTemplate
				.execute(() -> mongoTemplate.execute("BATCH_SEQUENCES", collection -> collection
					.findOneAndUpdate(new Document("_id", sequenceName), new Document("$inc", new Document("count", 1)),
							new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER))
					.getLong("count")));
		}
		catch (RetryException e) {
			Throwable cause = e.getCause();
			if (cause instanceof DataAccessException ex) {
				throw ex;
			}
			else {
				throw new RuntimeException("Failed to retrieve next value of sequence", e);
			}
		}
	}

	@Override
	public int nextIntValue() throws DataAccessException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String nextStringValue() throws DataAccessException {
		throw new UnsupportedOperationException();
	}

}
