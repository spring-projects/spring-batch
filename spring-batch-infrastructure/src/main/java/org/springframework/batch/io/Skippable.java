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

package org.springframework.batch.io;

/**
 * Implementation of this interface indicates to the framework that this object
 * is capable of skipping a record in cases where it cannot be processed because
 * it is invalid, incomplete for other non critical reasons.
 * 
 * @author Waseem Malik
 * @author Dave Syer
 * 
 */
public interface Skippable {

	/**
	 * Skip the current record. This method can be invoked whenever an input
	 * source provides an invalid object. The implementing class should skip the
	 * current record the next time it is encountered in the same process (e.g.
	 * after a rollback and retry of a transaction).
	 * 
	 */
	public void skip();

}
