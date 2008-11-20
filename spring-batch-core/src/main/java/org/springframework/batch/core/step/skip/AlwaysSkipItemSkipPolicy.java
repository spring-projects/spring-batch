/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.step.skip;


/**
 * Implementation of the {@link SkipPolicy} interface that
 * will always return that an item should be skipped.
 * 
 * @author Ben Hale
 * @author Lucas Ward
 */
public class AlwaysSkipItemSkipPolicy implements SkipPolicy {

	public boolean shouldSkip(Throwable t, int skipCount) {
		return true;
	}

}
