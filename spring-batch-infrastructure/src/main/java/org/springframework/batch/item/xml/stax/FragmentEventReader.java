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

package org.springframework.batch.item.xml.stax;

import javax.xml.stream.XMLEventReader;


/**
 * Interface for event readers which support treating XML fragments as standalone XML documents
 * by wrapping the fragments with StartDocument and EndDocument events.
 * 
 * @author Robert Kasanicky
 */
public interface FragmentEventReader extends XMLEventReader {

	/**
	 * Tells the event reader its cursor position is exactly before the fragment.
	 */
	void markStartFragment();
	
	/**
	 * Tells the event reader the current fragment has been processed.
	 * If the cursor is still inside the fragment it should be moved
	 * after the end of the fragment.
	 */
	void markFragmentProcessed();
	
	/**
	 * Reset the state of the fragment reader - make it forget
	 * it assumptions about current position of cursor
	 * (e.g. in case of rollback of the wrapped reader).
	 */
	void reset();

}
