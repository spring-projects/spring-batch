/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.core.aot;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.batch.core.repository.persistence.ExecutionContext;
import org.springframework.batch.core.repository.persistence.ExitStatus;
import org.springframework.batch.core.repository.persistence.JobExecution;
import org.springframework.batch.core.repository.persistence.JobInstance;
import org.springframework.batch.core.repository.persistence.JobParameter;
import org.springframework.batch.core.repository.persistence.StepExecution;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreRuntimeHintsTests {

	private RuntimeHints runtimeHints;

	@BeforeEach
	void setUp() {
		this.runtimeHints = new RuntimeHints();
		new CoreRuntimeHints().registerHints(this.runtimeHints, getClass().getClassLoader());
	}

	@Test
	void testMongoDbSchemaResourcesAreRegistered() {
		Stream
			.of("org/springframework/batch/core/schema-mongodb.jsonl",
					"org/springframework/batch/core/schema-mongodb.js",
					"org/springframework/batch/core/schema-drop-mongodb.jsonl",
					"org/springframework/batch/core/schema-drop-mongodb.js")
			.forEach(resource -> {
				assertNotNull(getClass().getClassLoader().getResource(resource),
						() -> "Resource not found: " + resource);
				assertTrue(RuntimeHintsPredicates.resource().forResource(resource).test(this.runtimeHints),
						() -> "Resource hint not found for " + resource);
			});
	}

	@Test
	void testMongoDbPersistenceTypesAreRegisteredForReflection() {
		Stream
			.of(JobInstance.class, ExecutionContext.class, ExitStatus.class, StepExecution.class, JobExecution.class,
					JobParameter.class)
			.forEach(type -> assertTrue(RuntimeHintsPredicates.reflection()
				.onType(type)
				.withMemberCategories(MemberCategory.values())
				.test(this.runtimeHints), () -> "Reflection hint not found for " + type.getName()));
	}

}
