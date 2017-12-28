/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.value;

/**
 * An object holder that holds a only one, or a stack of currently used objects for a path.
 * <p>
 * If the CurrentObject is a stack, then only the one on the top, the current object is in active use.
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public interface CurrentObject {
	
	/**
	 * Is there a current Object?
	 * 
	 * @return ansers the question
	 */
	boolean isEmpty();

	/**
	 * emit the next value, clears or pop from the current object
	 * 
	 * @return the current Object
	 */
	Object popCurrentObject();

	/**
	 * get the current Object
	 * 
	 * @return the current Object
	 */

	Object peekCurrentObject();

	/**
	 * set the current Object
	 * 
	 * @param current the object that is pushed to the CurrentObject
	 */
	void pushCurrentObject(Object current);

}
