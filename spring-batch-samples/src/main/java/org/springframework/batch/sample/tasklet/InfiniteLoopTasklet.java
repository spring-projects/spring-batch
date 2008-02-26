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

package org.springframework.batch.sample.tasklet;

import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.support.PropertiesConverter;

/**
 * Simple module implementation that will always return true to indicate that
 * processing should continue. This is useful for testing graceful shutdown of
 * jobs.
 * 
 * @author Lucas Ward
 * 
 */
public class InfiniteLoopTasklet implements Tasklet {

	private int count = 0;

	/**
	 * 
	 */
	public InfiniteLoopTasklet() {
		super();
	}

	public ExitStatus execute() throws Exception {
		Thread.sleep(500);
		count++;
		return ExitStatus.CONTINUABLE;
	}

}
