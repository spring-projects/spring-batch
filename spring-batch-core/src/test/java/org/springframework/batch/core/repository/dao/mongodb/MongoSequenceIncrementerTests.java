/*
 * Copyright 2026-present the original author or authors.
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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.data.mongodb.core.CollectionCallback;
import org.springframework.data.mongodb.core.MongoOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
class MongoSequenceIncrementerTests {

	private final MongoOperations mongoOperations = Mockito.mock(MongoOperations.class);

	@SuppressWarnings("unchecked")
	private final MongoCollection<Document> collection = Mockito.mock(MongoCollection.class);

	private final ArgumentCaptor<String> collectionNameCaptor = ArgumentCaptor.forClass(String.class);

	private final ArgumentCaptor<Bson> filterCaptor = ArgumentCaptor.forClass(Bson.class);

	@SuppressWarnings("unchecked")
	private void stubMongoOperations() {
		Mockito.when(this.collection.findOneAndUpdate(this.filterCaptor.capture(), Mockito.any(Bson.class),
				Mockito.any(FindOneAndUpdateOptions.class)))
			.thenReturn(new Document("count", 42L));
		Mockito.when(this.mongoOperations.execute(this.collectionNameCaptor.capture(), Mockito.any()))
			.thenAnswer(
					invocation -> ((CollectionCallback<Long>) invocation.getArgument(1)).doInCollection(collection));
	}

	@Test
	void testDefaultSequencesCollectionName() {
		// given
		stubMongoOperations();
		MongoSequenceIncrementer incrementer = new MongoSequenceIncrementer(this.mongoOperations,
				"BATCH_JOB_INSTANCE_SEQ");

		// when
		long value = incrementer.nextLongValue();

		// then
		assertEquals(42L, value);
		assertEquals("BATCH_SEQUENCES", this.collectionNameCaptor.getValue());
	}

	/*
	 * The sequence name is expected to be used as given: it is the caller's
	 * responsibility to qualify it with the collection prefix, if any.
	 */
	@Test
	void testSequenceNameIsNotAlteredByTheIncrementer() {
		// given
		stubMongoOperations();
		MongoSequenceIncrementer incrementer = new MongoSequenceIncrementer(this.mongoOperations,
				"BATCH_JOB_INSTANCE_SEQ");

		// when
		incrementer.nextLongValue();

		// then
		assertEquals(new Document("_id", "BATCH_JOB_INSTANCE_SEQ"), this.filterCaptor.getValue());
	}

	@Test
	void testCustomCollectionPrefix() {
		// given
		stubMongoOperations();
		MongoSequenceIncrementer incrementer = new MongoSequenceIncrementer(this.mongoOperations,
				"MY_APP_JOB_INSTANCE_SEQ");
		incrementer.setCollectionPrefix("MY_APP_");

		// when
		incrementer.nextLongValue();

		// then
		assertEquals("MY_APP_SEQUENCES", this.collectionNameCaptor.getValue());
		assertEquals(new Document("_id", "MY_APP_JOB_INSTANCE_SEQ"), this.filterCaptor.getValue());
	}

	@Test
	void testCollectionPrefixMustNotBeNull() {
		MongoSequenceIncrementer incrementer = new MongoSequenceIncrementer(this.mongoOperations, "SEQ");

		assertThrows(IllegalArgumentException.class, () -> incrementer.setCollectionPrefix(null));
	}

}
