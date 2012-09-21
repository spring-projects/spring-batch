package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import com.ibatis.sqlmap.client.SqlMapClient;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "JdbcPagingItemReaderCommonTests-context.xml")
public class IbatisPagingItemReaderAsyncTests {

	/**
	 * The number of items to read
	 */
	private static final int ITEM_COUNT = 1000;

	/**
	 * The number of threads to create
	 */
	private static final int THREAD_COUNT = 10;

	private static Log logger = LogFactory.getLog(IbatisPagingItemReaderAsyncTests.class);

	@Autowired
	private DataSource dataSource;

	private int maxId;

	@Before
	public void init() {
		SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
		maxId = jdbcTemplate.queryForInt("SELECT MAX(ID) from T_FOOS");
		for (int i = ITEM_COUNT; i > maxId; i--) {
			jdbcTemplate.update("INSERT into T_FOOS (ID,NAME,VALUE) values (?, ?, ?)", i, "foo" + i, i);
		}
		assertEquals(ITEM_COUNT, SimpleJdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS"));
	}

	@After
	public void destroy() {
		SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
		jdbcTemplate.update("DELETE from T_FOOS where ID>?", maxId);
	}

	@Test
	public void testAsyncReader() throws Throwable {
		List<Throwable> throwables = new ArrayList<Throwable>();
		int max = 10;
		for (int i = 0; i < max; i++) {
			try {
				logger.info("Testing asynch reader, iteration="+i);
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
		final IbatisPagingItemReader<Foo> reader = getItemReader();
		CompletionService<List<Foo>> completionService = new ExecutorCompletionService<List<Foo>>(Executors
				.newFixedThreadPool(THREAD_COUNT));
		for (int i = 0; i < THREAD_COUNT; i++) {
			completionService.submit(new Callable<List<Foo>>() {
				public List<Foo> call() throws Exception {
					List<Foo> list = new ArrayList<Foo>();
					Foo next = null;
					do {
						next = reader.read();
						Thread.sleep(10L); // try to make it fairer
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
		Set<Foo> results = new HashSet<Foo>();
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
		reader.close();
	}

	private IbatisPagingItemReader<Foo> getItemReader() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(dataSource);
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader<Foo> reader = new IbatisPagingItemReader<Foo>();
		if ("postgres".equals(System.getProperty("ENVIRONMENT"))) {
			reader.setQueryId("getPagedFoosPostgres");			
		} else if ("oracle".equals(System.getProperty("ENVIRONMENT"))) {
			reader.setQueryId("getPagedFoosOracle");			
		} else {
			reader.setQueryId("getPagedFoos");
		}
		reader.setPageSize(2);
		reader.setSqlMapClient(sqlMapClient);
		reader.setSaveState(true);

		reader.afterPropertiesSet();

		return reader;
	}

	private SqlMapClient createSqlMapClient() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(dataSource);
		factory.afterPropertiesSet();
		return (SqlMapClient) factory.getObject();
	}

}
