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
import java.lang.reflect.Method;

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
 * <p>
 * If the underlying repository needs to be flushed after writing (for example
 * {@code JpaRepository}), this can be achieved in one of two ways: if the repository
 * exposes a single method that both saves and flushes items, such as
 * {@code JpaRepository#saveAllAndFlush(Iterable)}, that method name can be set with
 * {@link #setMethodName(java.lang.String)} and it will be invoked once with the whole
 * chunk. Otherwise, if saving and flushing are separate operations, {@link #flush()} can
 * be overridden in a subclass to perform the flush after the chunk has been written.
 * </p>
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.2
 */
public class RepositoryItemWriter<T> implements ItemWriter<T>, InitializingBean {

	protected static final Log logger = LogFactory.getLog(RepositoryItemWriter.class);

	protected CrudRepository<T, ?> repository;

	private @Nullable String methodName;

	private boolean methodNameAcceptsIterable;

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
	 * Specifies what method on the repository to call. This method must either accept the
	 * type of object passed to this writer as its <em>sole</em> argument, in which case
	 * it is invoked once per item, or accept an {@link Iterable} as its <em>sole</em>
	 * argument (for example {@code saveAll} or {@code saveAllAndFlush}), in which case it
	 * is invoked once with the whole chunk.
	 * @param methodName {@link String} containing the method name.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
		this.methodNameAcceptsIterable = false;
	}

	/**
	 * Set the {@link org.springframework.data.repository.CrudRepository} implementation
	 * for persistence.
	 * @param repository the Spring Data repository to be set
	 * @deprecated since 6.1 in favor of passing the repository to the constructor.
	 * Scheduled for removal in 7.0.
	 */
	@Deprecated(since = "6.1", forRemoval = true)
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
			flush();
		}
	}

	/**
	 * Flush the repository if necessary. This method is a no-op by default, but can be
	 * overridden by a subclass to flush the underlying repository when it exposes a
	 * separate flush operation, rather than a single method that both saves and flushes
	 * (such as {@code JpaRepository#saveAllAndFlush}, which can be used directly via
	 * {@link #setMethodName}).
	 * @since 6.1
	 */
	protected void flush() {
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

		if (this.methodNameAcceptsIterable) {
			MethodInvoker invoker = createMethodInvoker(repository, methodName);
			invoker.setArguments(items.getItems());
			doInvoke(invoker);
			return;
		}

		MethodInvoker invoker = createMethodInvoker(repository, methodName);

		for (T object : items) {
			invoker.setArguments(object);
			doInvoke(invoker);
		}
	}

	/**
	 * Determine whether the given method on the repository accepts a single
	 * {@link Iterable} argument, such as {@code saveAll} or {@code saveAllAndFlush}. Such
	 * methods are invoked once with the whole chunk, instead of once per item.
	 * @param methodName the name of the method to look up on the repository
	 * @return {@code true} if the repository has a single-argument method with that name
	 * whose parameter type is assignable from {@link Iterable}
	 */
	private boolean acceptsIterable(String methodName) {
		for (Method method : this.repository.getClass().getMethods()) {
			if (method.getName().equals(methodName) && method.getParameterCount() == 1
					&& Iterable.class.isAssignableFrom(method.getParameterTypes()[0])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check mandatory properties - there must be a repository.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.methodName != null) {
			Assert.state(StringUtils.hasText(this.methodName), "methodName must not be empty.");
			this.methodNameAcceptsIterable = acceptsIterable(this.methodName);
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
