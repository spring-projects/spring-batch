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
package org.springframework.batch.core.resource;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lucas Ward
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"/org/springframework/batch/core/resource/ListPreparedStatementSetterTests-context.xml",
		"/org/springframework/batch/core/repository/dao/data-source-context.xml" })
public class ListPreparedStatementSetterTests {

	ListPreparedStatementSetter pss;

	StepExecution stepExecution;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Autowired
	private AbstractJob job;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private FooStoringItemWriter fooStoringItemWriter;

	@Before
	public void onSetUpInTransaction() throws Exception {

		pss = new ListPreparedStatementSetter();
		List<Long> parameters = new ArrayList<Long>();
		parameters.add(1L);
		parameters.add(4L);
		pss.setParameters(parameters);
	}

	@Transactional
	@Test
	public void testSetValues() {

		final List<String> results = new ArrayList<String>();
		simpleJdbcTemplate.getJdbcOperations().query("SELECT NAME from T_FOOS where ID > ? and ID < ?", pss,
				new RowCallbackHandler() {
					public void processRow(ResultSet rs) throws SQLException {
						results.add(rs.getString(1));
					}
				});

		assertEquals(2, results.size());
		assertEquals("bar2", results.get(0));
		assertEquals("bar3", results.get(1));
	}

	@Transactional
	@Test(expected = IllegalArgumentException.class)
	public void testAfterPropertiesSet() throws Exception {
		pss.setParameters(null);
		pss.afterPropertiesSet();
	}

	@Test
	public void testXmlConfiguration() throws Exception {
		this.simpleJdbcTemplate.update("create table FOO (ID integer, NAME varchar(40), VALUE integer)");
		try {
			this.simpleJdbcTemplate.update("insert into FOO values (?,?,?)", 0, "zero", 0);
			this.simpleJdbcTemplate.update("insert into FOO values (?,?,?)", 1, "one", 1);
			this.simpleJdbcTemplate.update("insert into FOO values (?,?,?)", 2, "two", 2);
			this.simpleJdbcTemplate.update("insert into FOO values (?,?,?)", 3, "three", 3);

			JobParametersBuilder builder = new JobParametersBuilder().addLong("min.id", 1L).addLong("max.id", 2L);
			JobExecution jobExecution = this.jobLauncher.run(this.job, builder.toJobParameters());
			assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

			List<Foo> foos = fooStoringItemWriter.getFoos();
			assertEquals(2, foos.size());
			System.err.println(foos.get(0));
			System.err.println(foos.get(1));
			assertEquals(new Foo(1, "one", 1), foos.get(0));
			assertEquals(new Foo(2, "two", 2), foos.get(1));
		}
		finally {
			this.simpleJdbcTemplate.update("drop table FOO");
		}
	}

	public static class FooStoringItemWriter implements ItemWriter<Foo> {
		private List<Foo> foos = new ArrayList<Foo>();

		public void write(List<? extends Foo> items) throws Exception {
			foos.addAll(items);
		}

		public List<Foo> getFoos() {
			return foos;
		}
	}
}
