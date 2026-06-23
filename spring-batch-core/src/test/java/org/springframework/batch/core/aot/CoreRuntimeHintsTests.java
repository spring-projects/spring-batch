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
package org.springframework.batch.core.aot;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.resource;

class CoreRuntimeHintsTests {

	@Test
	void mongoDbHintsAreRegistered() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new CoreRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());

		Stream<String> mongoDbResources = Stream.of("org/springframework/batch/core/schema-mongodb.jsonl",
				"org/springframework/batch/core/schema-mongodb.js",
				"org/springframework/batch/core/schema-drop-mongodb.jsonl",
				"org/springframework/batch/core/schema-drop-mongodb.js");
		mongoDbResources.forEach(path -> assertThat(runtimeHints).matches(resource().forResource(path)));
		Stream<Class<?>> persistenceTypes = Stream.of(
				org.springframework.batch.core.repository.persistence.ExecutionContext.class,
				org.springframework.batch.core.repository.persistence.ExitStatus.class,
				org.springframework.batch.core.repository.persistence.JobExecution.class,
				org.springframework.batch.core.repository.persistence.JobInstance.class,
				org.springframework.batch.core.repository.persistence.JobParameter.class,
				org.springframework.batch.core.repository.persistence.StepExecution.class);
		persistenceTypes.forEach(type -> assertThat(runtimeHints).matches(reflection().onType(type)
			.withMemberCategories(MemberCategory.ACCESS_DECLARED_FIELDS, MemberCategory.ACCESS_PUBLIC_FIELDS,
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS,
					MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS)));
	}

}
