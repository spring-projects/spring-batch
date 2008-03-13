/*
 * Copyright 2006-2008 the original author or authors.
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

import org.springframework.batch.item.ItemWriter;

/**
 * Listener interface around the writing of an item.
 * 
 * @author Lucas Ward
 * 
 */
public interface ItemWriteListener extends BatchListener {

	/**
	 * Called before {@link ItemWriter#write(Object)}
	 * 
	 * @param item to be written
	 */
	void beforeWrite(Object item);

	/**
	 * Called after {@link ItemWriter#write(Object)  If  the item is last in a
	 * chunk, this will be called before any transaction is committed, and
	 * before {@link ChunkListener#afterChunk()}
	 * @param item TODO
	 */
	void afterWrite(Object item);

	/**
	 * Called if an error occurs while trying to write.
	 * 
	 * @param ex
	 *            thrown from {@link ItemWriter}
	 * @param item
	 *            attempted to be written.
	 */
	void onWriteError(Exception ex, Object item);
}
