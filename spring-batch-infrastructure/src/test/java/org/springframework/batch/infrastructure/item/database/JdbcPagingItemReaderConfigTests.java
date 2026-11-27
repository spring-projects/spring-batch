/*
 * Copyright 2008-2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.support.HsqlPagingQueryProvider;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig
class JdbcPagingItemReaderConfigTests {

	@Autowired
	private JdbcPagingItemReader<Object> jdbcPagingItemReader;

	@Autowired
	private DataSource dataSource;

	@Test
	void testConfig() {
		assertNotNull(jdbcPagingItemReader);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = (NamedParameterJdbcTemplate) ReflectionTestUtils
			.getField(jdbcPagingItemReader, "namedParameterJdbcTemplate");
		JdbcTemplate jdbcTemplate = (JdbcTemplate) namedParameterJdbcTemplate.getJdbcOperations();
		assertEquals(1000, jdbcTemplate.getMaxRows());
		assertEquals(100, jdbcTemplate.getFetchSize());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "ID >= ?", "ID >= :minId" })
	void testPagingWithProxiedJdbcTemplate(String whereClause) throws Exception {
		AtomicInteger queryCount = new AtomicInteger();
		ProxyFactory proxyFactory = new ProxyFactory(new JdbcTemplate(dataSource));
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.addAdvice((MethodInterceptor) invocation -> {
			if (invocation.getMethod().getName().equals("query")) {
				queryCount.incrementAndGet();
			}
			return invocation.proceed();
		});
		JdbcTemplate jdbcTemplate = (JdbcTemplate) proxyFactory.getProxy();
		HsqlPagingQueryProvider queryProvider = getQueryProvider();
		queryProvider.setWhereClause(whereClause);
		JdbcPagingItemReader<Integer> reader = new JdbcPagingItemReader<>(jdbcTemplate, queryProvider);
		if (!whereClause.isEmpty()) {
			reader.setParameterValues(Map.of("minId", 1));
		}
		reader.setRowMapper((rs, rowNum) -> rs.getInt("ID"));
		reader.setPageSize(2);
		reader.setSaveState(false);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
		try {
			for (int id = 1; id <= 5; id++) {
				assertEquals(Integer.valueOf(id), reader.read());
			}
			assertNull(reader.read());
			assertEquals(3, queryCount.get());
		}
		finally {
			reader.close();
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { JdbcPagingItemReader.VALUE_NOT_SET, 7 })
	void testInjectedJdbcTemplateConfiguration(int fetchSize) throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.setFetchSize(17);
		jdbcTemplate.setQueryTimeout(5);
		JdbcPagingItemReader<Integer> reader = new JdbcPagingItemReader<>(jdbcTemplate, getQueryProvider());
		reader.setPageSize(2);
		reader.setFetchSize(fetchSize);
		reader.afterPropertiesSet();

		NamedParameterJdbcTemplate namedParameterJdbcTemplate = (NamedParameterJdbcTemplate) ReflectionTestUtils
			.getField(reader, "namedParameterJdbcTemplate");
		assertSame(jdbcTemplate, namedParameterJdbcTemplate.getJdbcOperations());
		assertEquals(2, jdbcTemplate.getMaxRows());
		assertEquals(fetchSize == JdbcPagingItemReader.VALUE_NOT_SET ? 17 : fetchSize, jdbcTemplate.getFetchSize());
		assertEquals(5, jdbcTemplate.getQueryTimeout());
	}

	@Test
	void testJdbcTemplateConstructorValidation() {
		assertThrows(IllegalArgumentException.class,
				() -> new JdbcPagingItemReader<>((JdbcTemplate) null, getQueryProvider()));
		assertThrows(IllegalArgumentException.class,
				() -> new JdbcPagingItemReader<>(new JdbcTemplate(), getQueryProvider()));
		assertThrows(IllegalArgumentException.class,
				() -> new JdbcPagingItemReader<>(new JdbcTemplate(dataSource), null));
	}

	private HsqlPagingQueryProvider getQueryProvider() {
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("ID");
		queryProvider.setFromClause("T_FOOS");
		queryProvider.setSortKeys(Map.of("ID", Order.ASCENDING));
		return queryProvider;
	}

}
