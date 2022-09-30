/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.batch.core.observability;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Observation created around a step execution.
 *
 * @author Marcin Grzejszczak
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public enum BatchStepObservation implements ObservationDocumentation {

	BATCH_STEP_OBSERVATION {
		@Override
		public String getName() {
			return "spring.batch.step";
		}

		@Override
		public String getContextualName() {
			return "%s";
		}

		@Override
		public KeyName[] getLowCardinalityKeyNames() {
			return StepLowCardinalityTags.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return StepHighCardinalityTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.batch";
		}
	};

	enum StepLowCardinalityTags implements KeyName {

		/**
		 * Name of the Spring Batch step.
		 */
		STEP_NAME {
			@Override
			public String asString() {
				return "spring.batch.step.name";
			}
		},

		/**
		 * Type of the Spring Batch step.
		 */
		STEP_TYPE {
			@Override
			public String asString() {
				return "spring.batch.step.type";
			}
		},

		/**
		 * Name of the Spring Batch job enclosing the step.
		 */
		JOB_NAME {
			@Override
			public String asString() {
				return "spring.batch.step.job.name";
			}
		},

		/**
		 * Step status.
		 */
		STEP_STATUS {
			@Override
			public String asString() {
				return "spring.batch.step.status";
			}
		}

	}

	enum StepHighCardinalityTags implements KeyName {

		/**
		 * ID of the Spring Batch step execution.
		 */
		STEP_EXECUTION_ID {
			@Override
			public String asString() {
				return "spring.batch.step.executionId";
			}
		}

	}

}
