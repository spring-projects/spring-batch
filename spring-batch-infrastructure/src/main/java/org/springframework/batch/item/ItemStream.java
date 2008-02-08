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

package org.springframework.batch.item;

import org.springframework.batch.item.exception.MarkFailedException;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.exception.StreamException;

/**
 * <p>
 * Marker interface defining a contract for periodically storing state and
 * restoring from that state should an error occur.
 * <p>
 * 
 * <p>
 * The state that is stored is represented as {@link ExecutionAttributes} which
 * enforces a requirement that any restart data can be represented by a
 * Properties object. In general, the contract is that
 * {@link ExecutionAttributes} that is returned via the
 * {@link #getExecutionAttributes()} method will be given back to the
 * {@link #restoreFrom(ExecutionAttributes)} method, exactly as it was provided.
 * </p>
 * 
 * @author Dave Syer
 * 
 */
public interface ItemStream extends ExecutionAttributesProvider {

	/**
	 * Restore to the state given the provided {@link ExecutionAttributes}.
	 * This can be used to restart after a failure - hence not normally used
	 * more than once per call to {@link #open()}.
	 * 
	 * @param context
	 */
	void restoreFrom(ExecutionAttributes context);

	/**
	 * If any resources are needed for the stream to operate they need to be
	 * initialised here.
	 */
	void open() throws StreamException;

	/**
	 * If any resources are needed for the stream to operate they need to be
	 * destroyed here. Once this method has been called all other methods
	 * (except open) may throw an exception.
	 */
	void close() throws StreamException;

	/**
	 * Clients are expected to check this flag before calling mark or reset.<br/>
	 * 
	 * Implementations should also document explicitly, if mark is supported,
	 * how it will behave in a multi-threaded environment. Generally, if the
	 * stream is being accessed from multiple threads concurrently, it will have
	 * to manage that internally, and also reflect only the completed marks
	 * (independent of the order they happen) when
	 * {@link ExecutionAttributesProvider#getExecutionAttributes()} is called.
	 * 
	 * @return true if mark and reset are supported by the {@link ItemStream}
	 */
	boolean isMarkSupported();

	/**
	 * Mark the stream so that it can be reset later and the items backed out.
	 * After this method is called the result will be reflected in subsequent
	 * calls to {@link ExecutionAttributesProvider#getExecutionAttributes()}.<br/>
	 * 
	 * In a multi-threaded setting implementations have to ensure that only the
	 * state from the current thread is saved.
	 * 
	 * @throws UnsupportedOperationException if the operation is not supported
	 * @throws MarkFailedException if there is a problem with the mark. If a
	 * mark fails inside a transaction, it would be worrying, but not normally
	 * fatal.
	 */
	void mark() throws MarkFailedException;

	/**
	 * Reset the stream to the last mark. After a reset the stream state will be
	 * such that changes (items read or written) since the last call to mark
	 * will not be visible after a call to close.<br/>
	 * 
	 * In a multi-threaded setting implementations have to ensure that only the
	 * state from the current thread is reset.
	 * 
	 * @throws UnsupportedOperationException if the operation is not supported
	 * @throws ResetFailedException if there is a problem with the reset. If a
	 * reset fails inside a transaction, it would normally be fatal, and would
	 * leave the stream in an inconsistent state. So while this is an unchecked
	 * exception, it may be important for a client to catch it explicitly.
	 */
	void reset() throws ResetFailedException;
}
