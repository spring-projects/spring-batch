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


/**
 * Interface for listeners to the batch process. Implementers can provide
 * enhance the behaviour of a batch in small cross-cutting modules. The
 * framework provides callbacks at key points in the processing.
 * 
 * @author Dave Syer
 * 
 */
public interface RepeatListener {
	/**
	 * Called by the framework before each batch item. Implementers can halt a
	 * batch by setting the complete flag on the context.
	 * 
	 * @param context the current batch context.
	 */
	void before(RepeatContext context);

	/**
	 * Called by the framework after each item has been processed, unless the
	 * item processing results in an exception. This method is called as soon as
	 * the result is known.
	 * 
	 * @param context the current batch context
	 * @param result the result of the callback
	 */
	void after(RepeatContext context, RepeatStatus result);

	/**
	 * Called once at the start of a complete batch, before any items are
	 * processed. Implementers can use this method to acquire any resources that
	 * might be needed during processing. Implementers can halt the current
	 * operation by setting the complete flag on the context. To halt all
	 * enclosing batches (the whole job), the would need to use the parent
	 * context (recursively).
	 * 
	 * @param context the current batch context
	 */
	void open(RepeatContext context);

	/**
	 * Called when a repeat callback fails by throwing an exception. There will
	 * be one call to this method for each exception thrown during a repeat
	 * operation (e.g. a chunk).<br/>
	 * 
	 * There is no need to re-throw the exception here - that will be done by
	 * the enclosing framework.
	 * 
	 * @param context the current batch context
	 * @param e the error that was encountered in an item callback.
	 */
	void onError(RepeatContext context, Throwable e);

	/**
	 * Called once at the end of a complete batch, after normal or abnormal
	 * completion (i.e. even after an exception). Implementers can use this
	 * method to clean up any resources.
	 * 
	 * @param context the current batch context.
	 */
	void close(RepeatContext context);
}
