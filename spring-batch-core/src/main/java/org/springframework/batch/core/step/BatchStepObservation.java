/*
 * Copyright 2013-2021 the original author or authors.
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

import io.micrometer.api.instrument.docs.DocumentedObservation;
import io.micrometer.api.instrument.docs.TagKey;

enum BatchStepObservation implements DocumentedObservation {

	/**
	 * Observation created around a Job execution.
	 */
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
		public TagKey[] getLowCardinalityTagKeys() {
			return StepLowCardinalityTags.values();
		}

		@Override
		public TagKey[] getHighCardinalityTagKeys() {
			return StepHighCardinalityTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.batch";
		}
	};

	enum StepLowCardinalityTags implements TagKey {

		/**
		 * Name of the Spring Batch job.
		 */
		STEP_NAME {
			@Override
			public String getKey() {
				return "spring.batch.step.name";
			}
		},

		/**
		 * Type of the Spring Batch job.
		 */
		STEP_TYPE {
			@Override
			public String getKey() {
				return "spring.batch.step.type";
			}
		},

		/**
		 * Name of the Spring Batch job.
		 */
		JOB_NAME {
			@Override
			public String getKey() {
				return "spring.batch.job.name";
			}
		},

		/**
		 * Step status.
		 */
		STEP_STATUS {
			@Override
			public String getKey() {
				return "spring.batch.step.status";
			}
		}

	}

	enum StepHighCardinalityTags implements TagKey {

		/**
		 * ID of the Spring Batch execution.
		 */
		STEP_EXECUTION_ID {
			@Override
			public String getKey() {
				return "spring.batch.step.executionId";
			}
		}

	}

}
