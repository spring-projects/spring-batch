/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.engine.execution.BatchResult;

/**
 * @author Thomas Risberg
 * @author Will Schipp
 */
public class IbatisBatchItemWriterTests {

	private IbatisBatchItemWriter<Foo> writer = new IbatisBatchItemWriter<Foo>();

	private DataSource ds;

	private SqlMapClient smc;

	private String statementId = "updateFoo";

	@SuppressWarnings("unused")
	private class Foo {
		private Long id;
		private String bar;

		public Foo(String bar) {
			this.id = 1L;
			this.bar = bar;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getBar() {
			return bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Foo)) {
				return false;
			}
			Foo compare = (Foo) obj;
			if (this.bar.equals(compare.getBar())) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return bar.hashCode();
		}

	}

	@Before
	public void setUp() throws Exception {
		smc = mock(SqlMapClient.class);
		ds = mock(DataSource.class);
		writer.setStatementId(statementId);
		writer.setSqlMapClient(smc);
		writer.setDataSource(ds);
		writer.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.JdbcBatchItemWriter#afterPropertiesSet()}
	 * .
	 * @throws Exception
	 */
	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new IbatisBatchItemWriter<Foo>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'SqlMapClient'.", message.indexOf("SqlMapClient") >= 0);
		}
		writer.setSqlMapClient(smc);
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message does not contain 'statementId'.", message.indexOf("statementId") >= 0);
		}
		writer.setStatementId("statementId");
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteAndFlush() throws Exception {
		SqlMapSession sms = mock(SqlMapSession.class);
		when(smc.openSession()).thenReturn(sms);
		sms.close();
		when(sms.getCurrentConnection()).thenReturn(null);
		sms.setUserConnection(null);
		sms.startBatch();
		when(sms.update("updateFoo", new Foo("bar"))).thenReturn(-2);
		List<BatchResult> results = Collections.singletonList(new BatchResult("updateFoo", "update foo"));
		results.get(0).setUpdateCounts(new int[] {1});
		when(sms.executeBatchDetailed()).thenReturn(results);
		writer.write(Collections.singletonList(new Foo("bar")));
	}

	@Test
	public void testWriteAndFlushWithEmptyUpdate() throws Exception {
		SqlMapSession sms = mock(SqlMapSession.class);
		when(smc.openSession()).thenReturn(sms);
		sms.close();
		when(sms.getCurrentConnection()).thenReturn(null);
		sms.setUserConnection(null);
		sms.startBatch();
		when(sms.update("updateFoo", new Foo("bar"))).thenReturn(1);
		List<BatchResult> results = Collections.singletonList(new BatchResult("updateFoo", "update foo"));
		results.get(0).setUpdateCounts(new int[] {0});
		when(sms.executeBatchDetailed()).thenReturn(results);
		try {
			writer.write(Collections.singletonList(new Foo("bar")));
			fail("Expected EmptyResultDataAccessException");
		}
		catch (EmptyResultDataAccessException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("did not update") >= 0);
		}
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("ERROR");
		SqlMapSession sms = mock(SqlMapSession.class);
		when(smc.openSession()).thenReturn(sms);
		sms.close();
		when(sms.getCurrentConnection()).thenReturn(null);
		sms.setUserConnection(null);
		sms.startBatch();
		when(sms.update("updateFoo", new Foo("bar"))).thenThrow(ex);
		try {
			writer.write(Collections.singletonList(new Foo("bar")));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("ERROR", e.getMessage());
		}
	}

}
