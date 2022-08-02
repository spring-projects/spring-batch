/*
 * Copyright 2009-2022 the original author or authors.
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
package org.springframework.batch.item.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

@SpringJUnitConfig(locations = "JdbcPagingItemReaderCommonTests-context.xml")
class JdbcPagingItemReaderAsyncTests {

	/**
	 * The page size
	 */
	private static final int PAGE_SIZE = 2;

	/**
	 * The number of items to read
	 */
	private static final int ITEM_COUNT = 10;

	/**
	 * The number of threads to create
	 */
	private static final int THREAD_COUNT = 3;

	private static final Log logger = LogFactory.getLog(JdbcPagingItemReaderAsyncTests.class);

	@Autowired
	private DataSource dataSource;

	private int maxId;

	@BeforeEach
	void init() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		Integer maxIdResult = jdbcTemplate.queryForObject("SELECT MAX(ID) from T_FOOS", Integer.class);
		maxId = maxIdResult == null ? 0 : maxIdResult;
		for (int i = maxId + 1; i <= ITEM_COUNT; i++) {
			jdbcTemplate.update("INSERT into T_FOOS (ID,NAME,VALUE) values (?, ?, ?)", i, "foo" + i, i);
		}
		assertEquals(ITEM_COUNT, JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS"));
	}

	@AfterEach
	void destroy() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update("DELETE from T_FOOS where ID>?", maxId);
	}

	@Test
	void testAsyncReader() {
		List<Throwable> throwables = new ArrayList<>();
		int max = 10;
		for (int i = 0; i < max; i++) {
			try {
				doTest();
			}
			catch (Throwable e) {
				throwables.add(e);
			}
		}
		if (!throwables.isEmpty()) {
			throw new IllegalStateException(String.format("Failed %d out of %d", throwables.size(), max),
					throwables.get(0));
		}
	}

	/**
	 * @throws Exception
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void doTest() throws Exception, InterruptedException, ExecutionException {
		final ItemReader<Foo> reader = getItemReader();
		CompletionService<List<Foo>> completionService = new ExecutorCompletionService<>(
				Executors.newFixedThreadPool(THREAD_COUNT));
		for (int i = 0; i < THREAD_COUNT; i++) {
			completionService.submit(new Callable<List<Foo>>() {
				@Override
				public List<Foo> call() throws Exception {
					List<Foo> list = new ArrayList<>();
					Foo next = null;
					do {
						next = reader.read();
						Thread.sleep(10L);
						logger.debug("Reading item: " + next);
						if (next != null) {
							list.add(next);
						}
					}
					while (next != null);
					return list;
				}
			});
		}
		int count = 0;
		Set<Foo> results = new HashSet<>();
		for (int i = 0; i < THREAD_COUNT; i++) {
			List<Foo> items = completionService.take().get();
			count += items.size();
			logger.debug("Finished items count: " + items.size());
			logger.debug("Finished items: " + items);
			assertNotNull(items);
			results.addAll(items);
		}
		assertEquals(ITEM_COUNT, count);
		assertEquals(ITEM_COUNT, results.size());
	}

	protected ItemReader<Foo> getItemReader() throws Exception {

		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<>();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);
		reader.setQueryProvider(queryProvider);
		reader.setRowMapper(new RowMapper<Foo>() {
			@Override
			public Foo mapRow(ResultSet rs, int i) throws SQLException {
				Foo foo = new Foo();
				foo.setId(rs.getInt(1));
				foo.setName(rs.getString(2));
				foo.setValue(rs.getInt(3));
				return foo;
			}
		});
		reader.setPageSize(PAGE_SIZE);
		reader.afterPropertiesSet();
		reader.setSaveState(false);

		return reader;
	}

}
