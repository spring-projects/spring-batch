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

package org.springframework.batch.item.file;

/**
 * Trivial implementation of {@link ResourceSuffixCreator} that uses the index
 * itself as suffix, separated by dot.
 * 
 * @author Robert Kasanicky
 */
public class SimpleResourceSuffixCreator implements ResourceSuffixCreator {

    @Override
	public String getSuffix(int index) {
		return "." + index;
	}

}
