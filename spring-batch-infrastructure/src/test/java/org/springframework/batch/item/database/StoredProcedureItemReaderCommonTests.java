/*
 * Copyright 2010-2023 the original author or authors.
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

import org.hsqldb.types.Types;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.sample.Foo;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.SqlParameter;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled("see FIXME in init-foo-schema.sql")
class StoredProcedureItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		StoredProcedureItemReader<Foo> result = new StoredProcedureItemReader<>(getDataSource(), "read_foos",
				new FooRowMapper());
		result.setVerifyCursorPosition(false);
		return result;
	}

	@Override
	protected void initializeContext() {
		ctx = new ClassPathXmlApplicationContext("data-source-context.xml");
	}

	@Test
	void testRestartWithDriverSupportsAbsolute() throws Exception {
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
		reader.setParameters(new SqlParameter[] { new SqlParameter("from_id", Types.NUMERIC),
				new SqlParameter("to_id", Types.NUMERIC) });
		reader.setPreparedStatementSetter(ps -> {
			ps.setInt(1, 1000);
			ps.setInt(2, 1001);
		});
		reader.setRowMapper(new FooRowMapper());
		reader.setVerifyCursorPosition(false);
		reader.open(new ExecutionContext());
	}

	@Test
	void testReadBeforeOpen() throws Exception {
		testedAsStream().close();
		tested = getItemReader();
		assertThrows(ReaderNotOpenException.class, tested::read);
	}

}
