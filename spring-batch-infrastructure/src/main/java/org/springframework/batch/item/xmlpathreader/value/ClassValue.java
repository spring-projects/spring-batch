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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;

/**
 * A Value that creates its object with a default constructor with the help of a Creator
 * 
 * @author Thomas Nill
 * @since 4.0.1
 * @see Value
 * @see Creator
 * 
 */
public class ClassValue extends Value {
	private static final Logger log = LoggerFactory.getLogger(ClassValue.class);

	/**
	 * Constructor
	 * 
	 * @param path the path to the {@link Value}
	 * @param clazz the clazz of the objects that are created
	 * @param current the global {@link CurrentObject}
	 * @param objectOfClass the {@link CurrentObject} that is used to hold the created objects
	 */
	public ClassValue(XmlElementPath path, Class<?> clazz, CurrentObject current, CurrentObject objectOfClass) {
		super(path, clazz, current, objectOfClass);
	}

	@Override
	public Object createNewObject() throws Exception {
		log.debug("create an object of class {} ", getClazz());
		return getClazz().newInstance();
	}

}
