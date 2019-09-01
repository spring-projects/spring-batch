/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.test.timeout;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

public class SleepingItemProcessor<I> implements ItemProcessor<I, I> {
	
	private long millisToSleep;

	@Nullable
	@Override
	public I process(I item) throws Exception {
		Thread.sleep(millisToSleep);
		return item;
	}
	
	public void setMillisToSleep(long millisToSleep) {
		this.millisToSleep = millisToSleep;
	}

}
