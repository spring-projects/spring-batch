/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.batch.core.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilderHelper;
import org.springframework.batch.core.step.job.JobStep;

/**
 * Tests for {@link StepBuilderHelperTests}.
 *
 * @author Andrey Litvitski
 */
class StepBuilderHelperTests {

	@Test
	void enhanceShouldDefaultStepNameToShortClassNameWhenNull() {
		JobRepository jobRepository = mock(JobRepository.class);
		TestStepBuilderHelper helper = new TestStepBuilderHelper(jobRepository);
		JobStep step = new JobStep(jobRepository);
		helper.enhanceForTest(step);
		assertThat(step.getName()).isEqualTo(step.getClass().getSimpleName());
	}

	static class TestStepBuilderHelper extends StepBuilderHelper<TestStepBuilderHelper> {

		public TestStepBuilderHelper(JobRepository jobRepository) {
			super(jobRepository);
		}

		@Override
		protected TestStepBuilderHelper self() {
			return this;
		}

		void enhanceForTest(AbstractStep step) {
			super.enhance(step);
		}

	}

}
