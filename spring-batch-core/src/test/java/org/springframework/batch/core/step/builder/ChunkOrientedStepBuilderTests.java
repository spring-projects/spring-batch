/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.step.builder;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.item.ChunkOrientedStep;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.jdbc.support.JdbcTransactionManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/**
 * @author Mahmoud Ben Hassine
 */
class ChunkOrientedStepBuilderTests {

	private final JobRepository jobRepository = mock(JobRepository.class);

	private final JdbcTransactionManager transactionManager = mock(JdbcTransactionManager.class);

	@Test
	void testDefaultSkipPolicyWhenOnlyRetryConfigured() throws Exception {
		// Given: ChunkOrientedStepBuilder with only retry configured (no skip)
		ChunkOrientedStep<String, String> step = new StepBuilder("testStep", jobRepository)
			.chunk(10, transactionManager)
			.reader(new ListItemReader<>(List.of("item1")))
			.writer(items -> {
			})
			.faultTolerant()
			.retry(Exception.class)
			.retryLimit(3)
			// No skip configuration!
			.build();

		// When: We get the SkipPolicy from the built step
		SkipPolicy skipPolicy = getSkipPolicyFromStep(step);

		// Then: It should be NeverSkipItemSkipPolicy (not AlwaysSkipItemSkipPolicy)
		assertInstanceOf(NeverSkipItemSkipPolicy.class, skipPolicy,
				"When only retry is configured, default SkipPolicy should be NeverSkipItemSkipPolicy "
						+ "to prevent silent data loss after retry exhaustion");
	}

	private SkipPolicy getSkipPolicyFromStep(ChunkOrientedStep<?, ?> step) throws Exception {
		Field skipPolicyField = ChunkOrientedStep.class.getDeclaredField("skipPolicy");
		skipPolicyField.setAccessible(true);
		return (SkipPolicy) skipPolicyField.get(step);
	}

}
