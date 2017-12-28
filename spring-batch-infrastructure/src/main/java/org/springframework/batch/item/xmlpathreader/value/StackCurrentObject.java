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

import java.util.ArrayDeque;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CurrentObject that is based on a Stack
 * <p>
 * If the path of a {@link org.springframework.batch.item.xmlpathreader.value.Value} is relative, then
 * the creation of the object for the XML can be recursive.
 * {@code
 *   <a name="A">
 *      <a name="B">
 *         <a name="C">
 *         </a>
 *      </a>
 *   </a>
 * }
 * <code><br><br>  
 *  {@literal @}XmlPath(path = "a")<br>
 * public class Child {<br><br>
 *               </code> 
 * creates three Child objects that are recursive created. They are pushed on a CurrentObject that is a stack. 
 * Only the one on the top, the current object is in active use.
 * @author Thomas Nill
 * @since 4.0.1
 * @see Stack
 *
 */
public class StackCurrentObject implements CurrentObject {
	private static final Logger log = LoggerFactory.getLogger(StackCurrentObject.class);

	private ArrayDeque<Object> current;

	/**
	 * Constructor
	 */
	public StackCurrentObject() {
		super();
		current = new ArrayDeque<>();
	}
	
	@Override
	public boolean isEmpty() {
		return current.isEmpty();
	}

	@Override
	public Object popCurrentObject() {
		if (current.isEmpty()) {
			return null;
		}
		return current.pop();
	}

	@Override
	public void pushCurrentObject(Object obj) {
		log.debug("push " + obj);
		this.current.push(obj);
	}

	@Override
	public Object peekCurrentObject() {
		log.debug("peek " + current.peek());
		return current.peek();
	}

}
