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
package org.springframework.batch.core;

/**
 * Interface for listener to skipped items. Callbacks will be called by
 * {@link Step} implementations at the appropriate time in the step lifecycle.
 * Callbacks must always be made (where relevant) in a transaction that is still
 * valid: i.e. possibly on a read error, but not on a write error.
 * 
 * @author Dave Syer
 * 
 */
public interface SkipListener extends StepListener {

	/**
	 * Callback for a failure on read that is legal, so is not going to be
	 * re-thrown.
	 * 
	 * @param t
	 */
	void onSkipInRead(Throwable t);

	/**
	 * This item failed on write with the given exception, and a skip was called
	 * for. The callback is deferred until a new transaction is available. This
	 * callback might occur more than once for the same item, but only once in
	 * successful transaction.
	 * 
	 * 
	 * @param item the failed item
	 * @param t the cause of the failure
	 */
	void onSkipInWrite(Object item, Throwable t);

}
