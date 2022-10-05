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
 * Observation created around a Job execution.
 *
 * @author Marcin Grzejszczak
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public enum BatchJobObservation implements ObservationDocumentation {

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
		public KeyName[] getLowCardinalityKeyNames() {
			return JobLowCardinalityTags.values();
		}

		@Override
		public KeyName[] getHighCardinalityKeyNames() {
			return JobHighCardinalityTags.values();
		}

		@Override
		public String getPrefix() {
			return "spring.batch";
		}
	};

	enum JobLowCardinalityTags implements KeyName {

		/**
		 * Name of the Spring Batch job.
		 */
		JOB_NAME {
			@Override
			public String asString() {
				return "spring.batch.job.name";
			}
		},

		/**
		 * Job status.
		 */
		JOB_STATUS {
			@Override
			public String asString() {
				return "spring.batch.job.status";
			}
		}

	}

	enum JobHighCardinalityTags implements KeyName {

		/**
		 * ID of the Spring Batch job instance.
		 */
		JOB_INSTANCE_ID {
			@Override
			public String asString() {
				return "spring.batch.job.instanceId";
			}
		},

		/**
		 * ID of the Spring Batch job execution.
		 */
		JOB_EXECUTION_ID {
			@Override
			public String asString() {
				return "spring.batch.job.executionId";
			}
		}

	}

}
