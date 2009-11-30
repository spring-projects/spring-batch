/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.poller;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Interface for polling a {@link Callable} instance provided by the user. Use
 * when you need to put something in the background (e.g. a remote invocation)
 * and wait for the result, e.g.
 * 
 * <pre>
 * Poller&lt;Result&gt; poller = ...
 * 
 * final long id = remoteService.execute(); // do something remotely
 * 
 * Future&lt;Result&gt; future = poller.poll(new Callable&lt;Result&gt; {
 *     public Object call() {
 *     	   // Look for the result (null if not ready)
 *     	   return remoteService.get(id);
 *     }
 * });
 * 
 * Result result = future.get(1000L, TimeUnit.MILLSECONDS);
 * </pre>
 * 
 * @author Dave Syer
 * 
 */
public interface Poller<T> {

	/**
	 * Use the callable provided to poll for a non-null result. The callable
	 * might be executed multiple times searching for a result, but once either
	 * a result or an exception has been observed the polling stops.
	 * 
	 * @param callable a {@link Callable} to use to retrieve a result
	 * @return a future which itself can be used to get the result
	 * 
	 */
	Future<T> poll(Callable<T> callable) throws Exception;

}
