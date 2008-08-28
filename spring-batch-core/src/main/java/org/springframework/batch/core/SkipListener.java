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
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public interface SkipListener<T,S> extends StepListener {

	/**
	 * Callback for a failure on read that is legal, so is not going to be
	 * re-thrown. In case transaction is rolled back and items are re-read, this
	 * callback will occur repeatedly for the same cause.
	 * 
	 * @param t cause of the failure
	 */
	void onSkipInRead(Throwable t);

	/**
	 * This item failed on write with the given exception, and a skip was called
	 * for. The callback occurs immediately after the item is marked for 
	 * skipping and is called only once for the same item, regardless of
	 * rollbacks (chunk may be re-processed several times or the exception on
	 * write may not cause rollback at all).
	 * 
	 * @param item the failed item
	 * @param t the cause of the failure
	 */
	void onSkipInWrite(S item, Throwable t);

	/**
	 * This item failed on processing with the given exception, and a skip was called
	 * for. The callback occurs immediately after the item is marked for 
	 * skipping and is called only once for the same item, regardless of
	 * rollbacks (chunk may be re-processed several times or the exception on
	 * write may not cause rollback at all).
	 * 
	 * @param item the failed item
	 * @param t the cause of the failure
	 */
	void onSkipInProcess(T item, Throwable t);

}
