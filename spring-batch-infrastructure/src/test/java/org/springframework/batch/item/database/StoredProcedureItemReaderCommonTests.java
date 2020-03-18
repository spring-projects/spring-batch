/*
 * Copyright 2010-2013 the original author or authors.
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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hsqldb.types.Types;
import org.junit.Test;
import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.sample.Foo;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameter;

@RunWith(JUnit4.class)
public class StoredProcedureItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

    @Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		StoredProcedureItemReader<Foo> result = new StoredProcedureItemReader<>();
		result.setDataSource(getDataSource());
		result.setProcedureName("read_foos");
		result.setRowMapper(new FooRowMapper());
		result.setVerifyCursorPosition(false);
		result.afterPropertiesSet();
		return result;
	}

    @Override
	protected void initializeContext() throws Exception {
		ctx = new ClassPathXmlApplicationContext("org/springframework/batch/item/database/stored-procedure-context.xml");
	}

	@Test
	public void testRestartWithDriverSupportsAbsolute() throws Exception {
		testedAsStream().close();
		tested = getItemReader();
		((StoredProcedureItemReader<Foo>) tested).setDriverSupportsAbsolute(true);
		testedAsStream().open(executionContext);
		testedAsStream().close();
		testedAsStream().open(executionContext);
		testRestart();
	}

    @Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		StoredProcedureItemReader<Foo> reader = (StoredProcedureItemReader<Foo>) tested;
		reader.close();
		reader.setDataSource(getDataSource());
		reader.setProcedureName("read_some_foos");
		reader.setParameters(
				new SqlParameter[] {
					new SqlParameter("from_id", Types.NUMERIC),
					new SqlParameter("to_id", Types.NUMERIC)	
				});
		reader.setPreparedStatementSetter(
				new PreparedStatementSetter() {
                    @Override
					public void setValues(PreparedStatement ps)
							throws SQLException {
						ps.setInt(1, 1000);
						ps.setInt(2, 1001);
					}
				});
		reader.setRowMapper(new FooRowMapper());
		reader.setVerifyCursorPosition(false);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());		
	}

	@Test(expected=ReaderNotOpenException.class)
	public void testReadBeforeOpen() throws Exception {
		testedAsStream().close();
		tested = getItemReader();
		tested.read();
	}
	
}
