/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.avro.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.avro.AvroItemReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 */
public abstract class AvroItemReaderTestSupport extends AvroTestFixtures {

	protected <T> void verify(AvroItemReader<T> avroItemReader, Chunk<T> actual) throws Exception {

		avroItemReader.open(new ExecutionContext());
		List<T> users = new ArrayList<>();

		T user;
		while ((user = avroItemReader.read()) != null) {
			users.add(user);
		}

		assertThat(users).hasSize(4);
		List<T> actualItems = actual.getItems();
		assertThat(users).containsExactlyInAnyOrder(actualItems.get(0), actualItems.get(1), actualItems.get(2),
				actualItems.get(3));

		avroItemReader.close();
	}

}
