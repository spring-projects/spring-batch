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
import org.springframework.util.Assert;

/**
 * A Value that creates the value object with the help of a Creator
 * 
 * @author Thomas Nill
 * @since 4.0.1
 * @see Creator
 */
public class CreatorValue extends Value {
	private static final Logger log = LoggerFactory.getLogger(CreatorValue.class);

	private Creator creator;

	/**
	 * Constructor
	 * 
	 * @param path the path to the {@link Value}
	 * @param clazz the clazz of the objects that are created
	 * @param current the global {@link CurrentObject}
	 * @param objectOfClass the {@link CurrentObject} that is used to hold the created objects
	 * @param creator the used Creator
	 */
	public CreatorValue(XmlElementPath path, Class<?> clazz, CurrentObject current, CurrentObject objectOfClass,
			Creator creator) {
		super(path, clazz, current, objectOfClass);
		Assert.notNull(creator, "The creator should not be null");

		this.creator = creator;
	}

	@Override
	public Object createNewObject() throws Exception {
		log.debug("call the creator {}", getPath());
		return creator.createObject();
	}

}
