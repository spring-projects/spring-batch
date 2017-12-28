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

import org.springframework.batch.item.xmlpathreader.path.PathEntryContainer;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;

/**
 * The ValueContainer contains Value Objects of the different XmlElementPath paths. Its a set of Values. It connects the
 * path, with the Value.
 * 
 * 
 * @author Thomas Nill
 * @since 4.0.1
 * @see ValueContainer
 * @see Value
 * @see XmlElementPath
 */
public class ValueContainer extends PathEntryContainer<Value> {

	/**
	 * constructor of parent class
	 */
	public ValueContainer() {
		super();
	}

	/**
	 * call a Value push method
	 * 
	 * @param path the path of the XML elements
	 */
	public void push(XmlElementPath path) {
		Value action = searchTheBestMatchingEntity(path);
		if (action != null) {
			action.push();
		}
	}

	/**
	 * call a Value pop method
	 * 
	 * @param path the path of the XML elements
	 */
	public void pop(XmlElementPath path) {
		Value action = searchTheBestMatchingEntity(path);
		if (action != null) {
			action.pop();
		}
	}

}
