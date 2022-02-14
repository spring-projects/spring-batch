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

package org.springframework.batch.core.job;

import io.micrometer.core.instrument.docs.DocumentedObservation;
import io.micrometer.core.instrument.docs.TagKey;

enum BatchJobObservation implements DocumentedObservation {

	/**
	 * Observation created around a Job execution.
	 */
	BATCH_JOB_OBSERVATION {
		@Override
		public String getName() {
			return "spring.batch.job";
		}

		@Override
		public String getContextualName() {
			return "%s";
		}

		@Override
		public TagKey[] getLowCardinalityTagKeys() {
			return JobLowCardinalityTags.values();
		}

		@Override
		public TagKey[] getHighCardinalityTagKeys() {
			return JobHighCardinalityTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.batch";
		}
	};

	enum JobLowCardinalityTags implements TagKey {

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
		 * Job status.
		 */
		JOB_STATUS {
			@Override
			public String getKey() {
				return "spring.batch.job.status";
			}
		}

	}

	enum JobHighCardinalityTags implements TagKey {

		/**
		 * ID of the Spring Batch job instance.
		 */
		JOB_INSTANCE_ID {
			@Override
			public String getKey() {
				return "spring.batch.job.instanceId";
			}
		},

		/**
		 * ID of the Spring Batch execution.
		 */
		JOB_EXECUTION_ID {
			@Override
			public String getKey() {
				return "spring.batch.job.executionId";
			}
		}

	}
}
