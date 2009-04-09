/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.repeat;

public enum RepeatStatus {

	/**
	 * Indicates that processing can continue.
	 */
	CONTINUABLE(true), 
	/**
	 * Indicates that processing is finished (either successful or unsuccessful)
	 */
	FINISHED(false);

	private final boolean continuable;

	private RepeatStatus(boolean continuable) {
		this.continuable = continuable;
	}

	public static RepeatStatus continueIf(boolean continuable) {
		return continuable ? CONTINUABLE : FINISHED;
	}

	public boolean isContinuable() {
		return this == CONTINUABLE;
	}

	public RepeatStatus and(boolean value) {
		return value && continuable ? CONTINUABLE : FINISHED;
	}

}
