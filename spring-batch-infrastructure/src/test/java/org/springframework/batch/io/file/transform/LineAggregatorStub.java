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

package org.springframework.batch.io.file.transform;

import org.springframework.batch.io.file.transform.LineAggregator;


/**
 * Stub implementation of {@link LineAggregator} interface for testing purposes.
 * 
 * @author robert.kasanicky
 */
public class LineAggregatorStub implements LineAggregator {

	/**
	 * Concatenates arguments. Ignores the LineDescriptor.
	 */
	public String aggregate(String[] args) {
		String result = "";

		for (int i = 1; i < args.length; i++) {
			result = result + args[i];
		}

		return result;
	}
}
