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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.utils.ClassUtils;
import org.springframework.util.Assert;

/**
 * A Value that creates its object with a static method of a Class.
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public class StaticMethodValue extends Value {
	private static final Logger log = LoggerFactory.getLogger(StaticMethodValue.class);

	private Method staticCreationMethod;

	/**
	 * Constructor
	 *
	 * @param path the path to the {@link Value}
	 * @param clazz the clazz of the objects that are created
	 * @param current the global {@link CurrentObject}
	 * @param staticMethodName the name of the static method
	 * @param objectOfClass the {@link CurrentObject} that is used to hold the created objects
	 */
	public StaticMethodValue(XmlElementPath path, Class<?> clazz, CurrentObject current, CurrentObject objectOfClass,
			String staticMethodName) {
		super(path, clazz, current, objectOfClass);
		Assert.hasText(staticMethodName, "The methodname should not be empty");

		log.debug("Create Value for path {} ", path);

		if (staticMethodName != null) {
			try {
				staticCreationMethod = clazz.getMethod(staticMethodName);
			}
			catch (NoSuchMethodException | SecurityException e) {
				log.debug("Error {}", e);
				Messages.throwIllegalArgumentException("Value.METHOD_DOESNOT_EXIST", staticMethodName);
			}
			if (!ClassUtils.isThisClassOrASuperClass(staticCreationMethod.getReturnType(), clazz)) {
				Messages.throwIllegalArgumentException("Value.METHOD_WRONG_TYPE", staticMethodName,
						clazz.getCanonicalName());
			}
			if (!Modifier.isStatic(staticCreationMethod.getModifiers())) {
				Messages.throwIllegalArgumentException("Value.METHOD_NOT_STATIC", staticMethodName);
			}
			if (staticCreationMethod.getParameterCount() > 0) {
				Messages.throwIllegalArgumentException("Value.METHOD_HAS_PARAMETER", staticMethodName);
			}
		}
	}

	@Override
	public Object createNewObject() throws Exception {
		return staticCreationMethod.invoke(null);
	}

}
