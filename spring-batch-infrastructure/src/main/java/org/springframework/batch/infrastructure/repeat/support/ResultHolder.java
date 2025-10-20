/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.repeat.support;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.RepeatContext;

/**
 * Interface for result holder.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @deprecated since 5.0 with no replacement. Scheduled for removal in 7.0.
 */
@Deprecated(since = "5.0", forRemoval = true)
interface ResultHolder {

	/**
	 * Get the result for client from this holder. Does not block if none is available
	 * yet.
	 * @return the result, or null if there is none.
	 */
	@Nullable RepeatStatus getResult();

	/**
	 * Get the error for client from this holder if any. Does not block if none is
	 * available yet.
	 * @return the error, or null if there is none.
	 */
	@Nullable Throwable getError();

	/**
	 * Get the context in which the result evaluation is executing.
	 * @return the context of the result evaluation.
	 */
	RepeatContext getContext();

}