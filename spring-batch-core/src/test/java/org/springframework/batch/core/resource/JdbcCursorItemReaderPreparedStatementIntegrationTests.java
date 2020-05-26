/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.batch.core.resource;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ArgumentPreparedStatementSetter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/core/repository/dao/data-source-context.xml")
public class JdbcCursorItemReaderPreparedStatementIntegrationTests {

	JdbcCursorItemReader<Foo> itemReader;

	private DataSource dataSource;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	@Before
	public void onSetUpInTransaction() throws Exception {
		
		itemReader = new JdbcCursorItemReader<>();
		itemReader.setDataSource(dataSource);
		itemReader.setSql("select ID, NAME, VALUE from T_FOOS where ID > ? and ID < ?");
		itemReader.setIgnoreWarnings(true);
		itemReader.setVerifyCursorPosition(true);
		
		itemReader.setRowMapper(new FooRowMapper());
		itemReader.setFetchSize(10);
		itemReader.setMaxRows(100);
		itemReader.setQueryTimeout(1000);
		itemReader.setSaveState(true);
		List<Long> parameters = new ArrayList<>();
		parameters.add(1L);
		parameters.add(4L);
		ArgumentPreparedStatementSetter pss = new ArgumentPreparedStatementSetter(parameters.toArray());

		itemReader.setPreparedStatementSetter(pss);
	}
	
	@Transactional @Test
	public void testRead() throws Exception{
		itemReader.open(new ExecutionContext());
		Foo foo = itemReader.read();
		assertEquals(2, foo.getId());
		foo = itemReader.read();
		assertEquals(3, foo.getId());
		assertNull(itemReader.read());
	}
	
}
