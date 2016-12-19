/*
 * Copyright 2012-2020 the original author or authors.
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
package org.springframework.batch.item.data;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator.InvocationTargetThrowableWrapper;
import org.springframework.batch.item.adapter.DynamicMethodInvocationException;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;

/**
 * <p>
 * A {@link org.springframework.batch.item.ItemReader} that reads records utilizing
 * a {@link org.springframework.data.repository.PagingAndSortingRepository}.
 * </p>
 *
 * <p>
 * Performance of the reader is dependent on the repository implementation, however
 * setting a reasonably large page size and matching that to the commit interval should
 * yield better performance.
 * </p>
 *
 * <p>
 * The reader must be configured with a {@link org.springframework.data.repository.PagingAndSortingRepository},
 * a {@link org.springframework.data.domain.Sort}, and a pageSize greater than 0.
 * </p>
 *
 * <p>
 * This implementation is thread-safe between calls to {@link #open(ExecutionContext)}, but remember to use
 * <code>saveState=false</code> if used in a multi-threaded client (no restart available).
 * </p>
 *
 * <p>It is important to note that this is a paging item reader and exceptions that are
 * thrown while reading the page itself (mapping results to objects, etc in the
 * {@link RepositoryItemReader#doPageRead()}) will not be skippable since this reader has
 * no way of knowing if an exception should be skipped and therefore will continue to read
 * the same page until the skip limit is exceeded.</p>
 *
 * <p>
 * NOTE: The {@code RepositoryItemReader} only reads Java Objects i.e. non primitives.
 * </p>
 *
 * @author Michael Minella
 * @author Antoine Kapps
 * @since 2.2
 */
public class RepositoryItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	private PagingAndSortingRepository<?, ?> repository;

	private Sort sort;

	private volatile int page = 0;

	private int pageSize = 10;

	private volatile int current = 0;

	private List<?> arguments;

	private volatile List<T> results;

	private final Object lock = new Object();

	private String methodName;

	public RepositoryItemReader() {
		setName(ClassUtils.getShortName(RepositoryItemReader.class));
	}

	/**
	 * Arguments to be passed to the data providing method.
	 *
	 * @param arguments list of method arguments to be passed to the repository
	 */
	public void setArguments(List<?> arguments) {
		this.arguments = arguments;
	}

	/**
	 * Provides ordering of the results so that order is maintained between paged queries
	 *
	 * @param sorts the fields to sort by and the directions
	 */
	public void setSort(Map<String, Sort.Direction> sorts) {
		this.sort = convertToSort(sorts);
	}

	/**
	 * @param pageSize The number of items to retrieve per page.
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * The {@link org.springframework.data.repository.PagingAndSortingRepository}
	 * implementation used to read input from.
	 *
	 * @param repository underlying repository for input to be read from.
	 */
	public void setRepository(PagingAndSortingRepository<?, ?> repository) {
		this.repository = repository;
	}

	/**
	 * Specifies what method on the repository to call.  This method must take
	 * {@link org.springframework.data.domain.Pageable} as the <em>last</em> argument.
	 *
	 * @param methodName name of the method to invoke
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(repository != null, "A PagingAndSortingRepository is required");
		Assert.state(pageSize > 0, "Page size must be greater than 0");
		Assert.state(sort != null, "A sort is required");
	}

	@Nullable
	@Override
	protected T doRead() throws Exception {

		synchronized (lock) {
			boolean nextPageNeeded = (results != null && current >= results.size());

			if (results == null || nextPageNeeded) {

				if (logger.isDebugEnabled()) {
					logger.debug("Reading page " + page);
				}

				results = doPageRead();
				page ++;

				if(results.size() <= 0) {
					return null;
				}

				if (nextPageNeeded) {
					current = 0;
				}
			}

			if(current < results.size()) {
				T curLine = results.get(current);
				current++;
				return curLine;
			}
			else {
				return null;
			}
		}
	}

	@Override
	protected void jumpToItem(int itemLastIndex) throws Exception {
		synchronized (lock) {
			page = itemLastIndex / pageSize;
			current = itemLastIndex % pageSize;
		}
	}

	/**
	 * Performs the actual reading of a page via the repository.
	 * Available for overriding as needed.
	 *
	 * @return the list of items that make up the page
	 * @throws Exception Based on what the underlying method throws or related to the
	 * 			calling of the method
	 */
	@SuppressWarnings("unchecked")
	protected List<T> doPageRead() throws Exception {
		Pageable pageRequest = PageRequest.of(page, pageSize, sort);

		MethodInvoker invoker = createMethodInvoker(repository, methodName);

		List<Object> parameters = new ArrayList<>();

		if(arguments != null && arguments.size() > 0) {
			parameters.addAll(arguments);
		}

		parameters.add(pageRequest);

		invoker.setArguments(parameters.toArray());

		Page<T> curPage = (Page<T>) doInvoke(invoker);

		return curPage.getContent();
	}

	@Override
	protected void doOpen() throws Exception {
	}

	@Override
	protected void doClose() throws Exception {
		synchronized (lock) {
			current = 0;
			page = 0;
			results = null;
		}
	}

	private Sort convertToSort(Map<String, Sort.Direction> sorts) {
		List<Sort.Order> sortValues = new ArrayList<>();

		for (Map.Entry<String, Sort.Direction> curSort : sorts.entrySet()) {
			sortValues.add(new Sort.Order(curSort.getValue(), curSort.getKey()));
		}

		return Sort.by(sortValues);
	}

	private Object doInvoke(MethodInvoker invoker) throws Exception{
		try {
			invoker.prepare();
		}
		catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new DynamicMethodInvocationException(e);
		}

		try {
			return invoker.invoke();
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
