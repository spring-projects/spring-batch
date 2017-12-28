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

package org.springframework.batch.item.xmlpathreader.path;

/**
 * A PathEntry is a entry in a PathEntryContainer It is connected to a XmlElementPath
 * 
 * @author Thomas Nill
 * @since 4.0.1
 * @see XmlElementPath
 * @see PathEntryContainer
 */
@FunctionalInterface
public interface PathEntry {
	/**
	 * get the Path to this entry
	 * 
	 * @return the path to this entry
	 */
	XmlElementPath getPath();
}
