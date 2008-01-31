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

/**
 * <p>
 * Marker interface defining a contract for periodically storing state and
 * restoring from that state should an error occur.
 * <p>
 * 
 * <p>
 * The state that is stored is represented as {@link StreamContext} which
 * enforces a requirement that any restart data can be represented by a
 * Properties object. In general, the contract is that {@link StreamContext}
 * that is returned via the {@link #getStreamContext()} method will be given
 * back to the {@link #restoreFrom(StreamContext)} method, exactly as it was
 * provided.
 * </p>
 * 
 * @author Dave Syer
 * 
 */
public interface ItemStream extends StreamContextProvider {

	/**
	 * Restart state given the provided {@link StreamContext}.
	 * 
	 * @param context
	 */
	void restoreFrom(StreamContext context);

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
	 * Clients are expected to check this flag before calling mark or reset.
	 * 
	 * @return true if mark and reset are supported by the {@link ItemStream}
	 */
	boolean isMarkSupported();

	/**
	 * Mark the stream so that it can be reset later and the items backed out.
	 * Implementations may use the information in the provided context to make
	 * calculations that account for things like multiple open cursors. The
	 * context should also be updated with any information of this nature that
	 * might be needed by a reset or by future calls to mark.
	 * 
	 * @param the context which might contain information needed to determine
	 * what action to take, and into which the current mark information can go.
	 * 
	 * @throws UnsupportedOperationException if the operation is not supported
	 */
	void mark(StreamContext streamContext);

	/**
	 * Reset the stream to the last mark. After a reset the stream state will be
	 * such that changes (items read or written) since the last call to mark
	 * with the same context will not be visible after a call to close.
	 * 
	 * @throws UnsupportedOperationException if the operation is not supported
	 */
	void reset(StreamContext streamContext);
}
