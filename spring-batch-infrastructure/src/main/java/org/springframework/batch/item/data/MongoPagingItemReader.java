/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.ClassUtils;

/**
 * <p>
 * Restartable {@link ItemReader} that reads documents from MongoDB via a paging
 * technique.
 * </p>
 *
 * <p>
 * If you set JSON String query {@link #setQuery(String)} then it executes the JSON to
 * retrieve the requested documents.
 * </p>
 *
 * <p>
 * If you set Query object {@link #setQuery(Query)} then it executes the Query to retrieve
 * the requested documents.
 * </p>
 *
 * <p>
 * The query is executed using paged requests specified in the {@link #setPageSize(int)}.
 * Additional pages are requested as needed to provide data when the {@link #read()}
 * method is called.
 * </p>
 *
 * <p>
 * The JSON String query provided supports parameter substitution via ?&lt;index&gt;
 * placeholders where the &lt;index&gt; indicates the index of the parameterValue to
 * substitute.
 * </p>
 *
 * <p>
 * The implementation is thread-safe between calls to {@link #open(ExecutionContext)}, but
 * remember to use <code>saveState=false</code> if used in a multi-threaded client (no
 * restart available).
 * </p>
 *
 * @param <T> type of items to read
 * @since 5.1
 * @author Michael Minella
 * @author Takaaki Iida
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
public class MongoPagingItemReader<T> extends MongoItemReader<T> {

	/**
	 * Create a new {@link MongoPagingItemReader}.
	 */
	public MongoPagingItemReader() {
		setName(ClassUtils.getShortName(MongoPagingItemReader.class));
	}

	@Override
	public void setTemplate(MongoOperations template) {
		super.setTemplate(template);
	}

	@Override
	public void setQuery(Query query) {
		super.setQuery(query);
	}

	@Override
	public void setQuery(String queryString) {
		super.setQuery(queryString);
	}

	@Override
	public void setTargetType(Class<? extends T> type) {
		super.setTargetType(type);
	}

	@Override
	public void setParameterValues(List<Object> parameterValues) {
		super.setParameterValues(parameterValues);
	}

	@Override
	public void setFields(String fields) {
		super.setFields(fields);
	}

	@Override
	public void setSort(Map<String, Sort.Direction> sorts) {
		super.setSort(sorts);
	}

	@Override
	public void setCollection(String collection) {
		super.setCollection(collection);
	}

	@Override
	public void setHint(String hint) {
		super.setHint(hint);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

	@Override
	protected Iterator<T> doPageRead() {
		return super.doPageRead();
	}

	@Override
	protected String replacePlaceholders(String input, List<Object> values) {
		return super.replacePlaceholders(input, values);
	}

	@Override
	protected Sort convertToSort(Map<String, Sort.Direction> sorts) {
		return super.convertToSort(sorts);
	}

}
