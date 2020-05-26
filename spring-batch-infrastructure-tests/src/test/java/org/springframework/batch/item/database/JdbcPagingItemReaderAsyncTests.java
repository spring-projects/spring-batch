/*
 * Copyright 2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Dave Syer
 * @author David Thexton
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "JdbcPagingItemReaderCommonTests-context.xml")
public class JdbcPagingItemReaderAsyncTests {

	/**
	 * The page size
	 */
	private static final int PAGE_SIZE = 10;

	/**
	 * The number of items to read
	 */
	private static final int ITEM_COUNT = 1000;

	/**
	 * The number of threads to create
	 */
	private static final int THREAD_COUNT = 10;

	private static Log logger = LogFactory.getLog(JdbcPagingItemReaderAsyncTests.class);

	@Autowired
	private DataSource dataSource;

	private int maxId;

	@Before
	public void init() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		Integer tempMaxId = jdbcTemplate.queryForObject("SELECT MAX(ID) from T_FOOS", Integer.class);
		maxId = tempMaxId != null? tempMaxId : 0;
		for (int i = ITEM_COUNT; i > maxId; i--) {
			jdbcTemplate.update("INSERT into T_FOOS (ID,NAME,VALUE) values (?, ?, ?)", i, "foo" + i, i);
		}
		assertEquals(ITEM_COUNT, JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS"));
	}

	@After
	public void destroy() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update("DELETE from T_FOOS where ID>?", maxId);
	}

	@Test
	public void testAsyncReader() throws Throwable {
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
			throw new IllegalStateException(String.format("Failed %d out of %d", throwables.size(), max), throwables
					.get(0));
		}
	}

	/**
	 * @throws Exception
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void doTest() throws Exception, InterruptedException, ExecutionException {
		final ItemReader<Foo> reader = getItemReader();
		CompletionService<List<Foo>> completionService = new ExecutorCompletionService<>(Executors
				.newFixedThreadPool(THREAD_COUNT));
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
					} while (next != null);
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
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		factory.setSelectClause("select ID, NAME, VALUE");
		factory.setFromClause("from T_FOOS");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("VALUE", Order.ASCENDING);
		factory.setSortKeys(sortKeys);
		reader.setQueryProvider(factory.getObject());
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
