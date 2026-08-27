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
package org.springframework.batch.core;

/**
 * Class that defines the Spring Batch constants.
 *
 * @author Yanming Zhou
 */
public final class BatchConstants {

	/**
	 * The key to use in the execution context for whether step is executed.
	 */
	public static final String BATCH_EXECUTED = "batch.executed";

	/**
	 * The key to use in the execution context for whether job or step is recovered.
	 */
	public static final String BATCH_RECOVERED = "batch.recovered";

	/**
	 * The key to use in the execution context for whether step is restarted.
	 */
	public static final String BATCH_RESTART = "batch.restart";

	/**
	 * The key to use in the execution context for step type.
	 */
	public static final String BATCH_STEP_TYPE = "batch.stepType";

	/**
	 * The key to use in the execution context for tasklet type.
	 */
	public static final String BATCH_TASKLET_TYPE = "batch.taskletType";

	/**
	 * The key to use in the execution context for batch version.
	 */
	public static final String BATCH_VERSION = "batch.version";

	private BatchConstants() {
	}

}