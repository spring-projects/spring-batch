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

package org.springframework.batch.item.xmlpathreader.utils;

import org.springframework.util.Assert;

/**
 * Helper Method for the checking of subclassing
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public class ClassUtils {

	private ClassUtils() {
		super();
	}

	/**
	 * check that the class clazz is of the expacted type
	 * 
	 * @param clazz the class that will be tested
	 * @param expectedType the expacted class
	 * @return is the clazz of type expectedType
	 */
	public static boolean isThisClassOrASuperClass(Class<?> clazz, Class<?> expectedType) {
		Assert.notNull(clazz, "The class should not be null");
		Assert.notNull(expectedType, "The class should not be null");

		if (clazz.equals(Void.class)) {
			return false;
		}
		if (clazz.equals(expectedType)) {
			return true;
		}
		Class<?> superClass = clazz.getSuperclass();
		if (superClass == null) {
			return false;
		}
		return isThisClassOrASuperClass(superClass, expectedType);
	}

}
