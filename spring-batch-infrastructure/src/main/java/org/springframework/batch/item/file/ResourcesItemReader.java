/*
 * Copyright 2009-2018 the original author or authors.
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
package org.springframework.batch.item.file;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ItemReader} which produces {@link Resource} instances from an array.
 * This can be used conveniently with a configuration entry that injects a
 * pattern (e.g. <code>mydir/*.txt</code>, which can then be converted by Spring
 * to an array of Resources by the ApplicationContext.
 * 
 * <br>
 * <br>
 * 
 * Thread-safe between calls to {@link #open(ExecutionContext)}. The
 * {@link ExecutionContext} is not accurate in a multi-threaded environment, so
 * do not rely on that data for restart (i.e. always open with a fresh context).
 * 
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 * @see ResourceArrayPropertyEditor
 * 
 * @since 2.1
 */
public class ResourcesItemReader extends AbstractItemStreamItemReader<Resource> {

	private Resource[] resources = new Resource[0];

	private AtomicInteger counter = new AtomicInteger(0);

        public ResourcesItemReader() {
		/*
		 * Initialize the name for the key in the execution context.
		 */
		this.setExecutionContextName(getClass().getName());
	}

	/**
	 * The resources to serve up as items. Hint: use a pattern to configure.
	 * 
	 * @param resources the resources
	 */
	public void setResources(Resource[] resources) {
		this.resources = Arrays.asList(resources).toArray(new Resource[resources.length]);
	}

	/**
	 * Increments a counter and returns the next {@link Resource} instance from
	 * the input, or {@code null} if none remain.
	 */
    @Override
	@Nullable
	public synchronized Resource read() throws Exception {
		int index = counter.incrementAndGet() - 1;
		if (index >= resources.length) {
			return null;
		}
		return resources[index];
	}

    @Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
                super.open(executionContext);
		counter.set(executionContext.getInt(getExecutionContextKey("COUNT"), 0));
	}

    @Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
                super.update(executionContext);
		executionContext.putInt(getExecutionContextKey("COUNT"), counter.get());
	}

}
