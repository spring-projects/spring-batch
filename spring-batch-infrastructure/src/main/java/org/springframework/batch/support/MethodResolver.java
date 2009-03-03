/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.batch.support;

import java.lang.reflect.Method;

/**
 * Strategy interface for detecting a single Method on a Class.
 * 
 * @author Mark Fisher
 */
public interface MethodResolver {

	/**
	 * Find a single Method on the provided Object that matches this resolver's
	 * criteria.
	 * 
	 * @param candidate the candidate Object whose Class should be searched for
	 * a Method
	 * 
	 * @return a single Method or <code>null</code> if no Method matching this
	 * resolver's criteria can be found.
	 * 
	 * @throws IllegalArgumentException if more than one Method defined on the
	 * given candidate's Class matches this resolver's criteria
	 */
	Method findMethod(Object candidate) throws IllegalArgumentException;

	/**
	 * Find a <em>single</em> Method on the given Class that matches this
	 * resolver's criteria.
	 * 
	 * @param clazz the Class instance on which to search for a Method
	 * 
	 * @return a single Method or <code>null</code> if no Method matching this
	 * resolver's criteria can be found.
	 * 
	 * @throws IllegalArgumentException if more than one Method defined on the
	 * given Class matches this resolver's criteria
	 */
	Method findMethod(Class<?> clazz);

}
