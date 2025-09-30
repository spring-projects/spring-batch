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

package org.springframework.batch.integration.chunk;

/**
 * Interface for a remote worker in the Remote Chunking pattern. A request comes from a
 * manager process containing some items to be processed. Once the items are done with a
 * response needs to be generated containing a summary of the result.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @param <T> the type of the items to be processed (it is recommended to use a Memento
 * like a primary key)
 */
public interface ChunkRequestHandler<T> {

	/**
	 * Handle the chunk request, processing all the items and returning a response
	 * summarising the result. If the result is a failure then the response should say so.
	 * The handler only throws an exception if it needs to roll back a transaction and
	 * knows that the request will be re-delivered (if not to the same handler then to one
	 * processing the same Step).
	 * @param chunk a request containing the chunk to process
	 * @return a response summarising the result
	 * @throws Exception if the handler needs to roll back a transaction and have the
	 * chunk re-delivered
	 */
	ChunkResponse handle(ChunkRequest<T> chunk) throws Exception;

}