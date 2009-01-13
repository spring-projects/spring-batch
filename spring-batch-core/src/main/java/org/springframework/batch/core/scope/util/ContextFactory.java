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

package org.springframework.batch.core.scope.util;

/**
 * Interface to allow the context root for placeholder resolution to be switched
 * at runtime.  Useful for testing.
 * 
 * @author Dave Syer
 * 
 */
public interface ContextFactory {

	/**
	 * @return a root object to which placeholders resolve relatively
	 */
	Object getContext();

	/**
	 * @return a unique identifier for this context
	 */
	String getContextId();

}
