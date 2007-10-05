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

package org.springframework.batch.item;

/**
 * Common interface for classes that require initialisation before they can be
 * used and need to free resources after they are no longer used.
 */
public interface ResourceLifecycle {
	/**
	 * This method should be invoked by clients at the start of processing to
	 * allow initialisation of resources.
	 * 
	 */
	public void open();

	/**
	 * This method should be invoked by clients after the completion of each
	 * step and the implementing class should close all managed resources.
	 * 
	 */
	public void close();
}
