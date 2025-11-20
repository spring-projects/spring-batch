/*
 * Copyright 2024-2025 the original author or authors.
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

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import org.bson.Document;

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

// Based on https://www.mongodb.com/blog/post/generating-globally-unique-identifiers-for-use-with-mongodb
// Section: Use a single counter document to generate unique identifiers one at a time

/**
 * @author Mahmoud Ben Hassine
 * @author Christoph Strobl
 * @since 5.2.0
 */
public class MongoSequenceIncrementer implements DataFieldMaxValueIncrementer {

	private static final String SEQUENCES_COLLECTION_NAME = "SEQUENCES";

	private final MongoOperations mongoTemplate;

	private final String sequenceName;

	private final String sequencesCollectionName;

	public MongoSequenceIncrementer(MongoOperations mongoTemplate, String sequenceName) {
		this(mongoTemplate, sequenceName, "BATCH_");
	}

	public MongoSequenceIncrementer(MongoOperations mongoTemplate, String sequenceName, String collectionPrefix) {
		this.mongoTemplate = mongoTemplate;
		this.sequencesCollectionName = collectionPrefix + SEQUENCES_COLLECTION_NAME;
		this.sequenceName = collectionPrefix + sequenceName;
	}

	@Override
	public long nextLongValue() throws DataAccessException {
		return mongoTemplate.execute(sequencesCollectionName,
				collection -> collection
					.findOneAndUpdate(new Document("_id", sequenceName), new Document("$inc", new Document("count", 1)),
							new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER))
					.getLong("count"));
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
