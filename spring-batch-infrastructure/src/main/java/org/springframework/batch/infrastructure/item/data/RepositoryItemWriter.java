/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item.data;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.adapter.AbstractMethodInvokingDelegator.InvocationTargetThrowableWrapper;
import org.springframework.batch.infrastructure.item.adapter.DynamicMethodInvocationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.StringUtils;

/**
 * <p>
 * A {@link ItemWriter} wrapper for a
 * {@link org.springframework.data.repository.CrudRepository} from Spring Data.
 * </p>
 *
 * <p>
 * By default, this writer will use {@link CrudRepository#saveAll(Iterable)} to save
 * items, unless another method is selected with {@link #setMethodName(java.lang.String)}.
 * It depends on
 * {@link org.springframework.data.repository.CrudRepository#saveAll(Iterable)} method to
 * store the items for the chunk. Performance will be determined by that implementation
 * more than this writer.
 * </p>
 *
 * <p>
 * As long as the repository provided is thread-safe, this writer is also thread-safe once
 * properties are set (normal singleton behavior), so it can be used in multiple
 * concurrent transactions.
 * </p>
 *
 * <p>
 * NOTE: The {@code RepositoryItemWriter} only stores Java Objects i.e. non primitives.
 * </p>
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.2
 */
public class RepositoryItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(RepositoryItemWriter.class);

	private CrudRepository<T, ?> repository;

	private @Nullable String methodName;

	/**
	 * Create a new {@link RepositoryItemWriter} instance with the provided repository.
	 * @param repository the Spring Data repository to be used for persistence.
	 * @since 6.0
	 */
	public RepositoryItemWriter(CrudRepository<T, ?> repository) {
		Assert.notNull(repository, "The CrudRepository must not be null");
		this.repository = repository;
	}

	/**
	 * Specifies what method on the repository to call. This method must have the type of
	 * object passed to this writer as the <em>sole</em> argument.
	 * @param methodName {@link String} containing the method name.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * Set the {@link org.springframework.data.repository.CrudRepository} implementation
	 * for persistence
	 * @param repository the Spring Data repository to be set
	 */
	public void setRepository(CrudRepository<T, ?> repository) {
		this.repository = repository;
	}

	/**
	 * Write all items to the data store via a Spring Data repository.
	 *
	 * @see ItemWriter#write(Chunk)
	 */
	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {
		if (!chunk.isEmpty()) {
			doWrite(chunk);
		}
	}

	/**
	 * Performs the actual write to the repository. This can be overridden by a subclass
	 * if necessary.
	 * @param items the list of items to be persisted.
	 * @throws Exception thrown if error occurs during writing.
	 */
	protected void doWrite(Chunk<? extends T> items) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Writing to the repository with " + items.size() + " items.");
		}

		if (this.methodName == null) {
			this.repository.saveAll(items);
			return;
		}

		MethodInvoker invoker = createMethodInvoker(repository, methodName);

		for (T object : items) {
			invoker.setArguments(object);
			doInvoke(invoker);
		}
	}

	/**
	 * Check mandatory properties - there must be a repository.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.methodName != null) {
			Assert.state(StringUtils.hasText(this.methodName), "methodName must not be empty.");
		}
		else {
			logger.debug("No method name provided, CrudRepository.saveAll will be used.");
		}
	}

	private void doInvoke(MethodInvoker invoker) throws Exception {
		try {
			invoker.prepare();
		}
		catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new DynamicMethodInvocationException(e);
		}

		try {
			invoker.invoke();
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			}
			else {
				throw new InvocationTargetThrowableWrapper(e.getCause());
			}
		}
		catch (IllegalAccessException e) {
			throw new DynamicMethodInvocationException(e);
		}
	}

	private MethodInvoker createMethodInvoker(Object targetObject, String targetMethod) {
		MethodInvoker invoker = new MethodInvoker();
		invoker.setTargetObject(targetObject);
		invoker.setTargetMethod(targetMethod);
		return invoker;
	}

}
