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

package org.springframework.batch.core.tasklet;

/**
 * Marker interface for {@link Tasklet} implementations that are able totake a
 * recovery action in the case that an exception is thrown inside
 * {@link Tasklet#execute()}. Containers must ensure that the recover method is
 * called in a different transactional context than the failed execution, e.g.
 * by creating a new transaction with propagation REQUIRES_NEW.
 * 
 * @author Dave Syer
 * 
 */
public interface Recoverable {

	/**
	 * Take some action to recover the current batch operation. E.g. send a
	 * message to an error queue, or append a bad record to a special file.
	 * 
	 * @param cause the exception that caused the recovery step to be called.
	 */
	void recover(Throwable cause);

}
