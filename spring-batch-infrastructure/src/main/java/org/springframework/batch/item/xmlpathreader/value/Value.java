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
import org.springframework.batch.item.xmlpathreader.exceptions.ReaderRuntimeException;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPathEntry;
import org.springframework.util.Assert;

/**
 * A Value holds a instance of a class. In the push method, this instance is generated and in the pop method this object
 * is transfered to the CurrentObject. The Value uses implementations of
 * {@link org.springframework.batch.item.xmlpathreader.value.CurrentObject}. If the path of the Value is absolute, the
 * objectValue is a {@link org.springframework.batch.item.xmlpathreader.value.SimpleCurrentObject} that holds only one
 * object. If the path is relative, then there can be recursive creation of objects and the objectValue is a
 * {@link org.springframework.batch.item.xmlpathreader.value.StackCurrentObject}
 * @author Thomas Nill
 * @since 4.0.1
 * @see CurrentObject
 * @see SimpleCurrentObject
 * @see StackCurrentObject
 */
public abstract class Value extends XmlElementPathEntry {
	private static final Logger log = LoggerFactory.getLogger(Value.class);

	/**
	 * The Class where the objects of this Value belongs too.
	 */
	private Class<?> clazz;

	/**
	 * A reference to a CurrentObject that holds the objects of this particular Value
	 * 
	 */
	private CurrentObject objectValue;

	/**
	 * A reference to the global CurrentObject. The pop method writes to this global object holder.
	 */
	private CurrentObject current;

	/**
	 * Constructor
	 * 
	 * @param path the path to the {@link Value}
	 * @param clazz the clazz of the objects that are created
	 * @param current the global {@link CurrentObject}
	 * @param objectValue the {@link CurrentObject} that is used to hold the created objects
	 */
	public Value(XmlElementPath path, Class<?> clazz, CurrentObject current, CurrentObject objectValue) {
		super(path);
		Assert.notNull(path, "The path should not be null");
		Assert.notNull(clazz, "The class should not be null");
		Assert.notNull(current, "The current should not be null");
		Assert.notNull(objectValue, "The objectValue should not be null");

		log.debug("Create Value for path {} ", path);
		this.clazz = clazz;
		this.current = current;
		this.objectValue = objectValue;
	}

	public Object getValue() {
		return objectValue.peekCurrentObject();
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public CurrentObject getCurrent() {
		return objectValue;
	}

	/**
	 * push method create a instance of class clazz
	 * 
	 */
	public void push() {
		try {
			objectValue.pushCurrentObject(createNewObject());
		}
		catch (Exception e) {
			throw new ReaderRuntimeException(e);
		}
	}

	/**
	 * pop action transfers object to CurrentObject
	 * 
	 */
	public void pop() {
		current.pushCurrentObject(objectValue.popCurrentObject());
	}

	/**
	 * create the new Object that would be written in the value
	 * 
	 * @return the new created object
	 * @throws Exception if the creation fails
	 */
	public abstract Object createNewObject() throws Exception;

}
