/*
 * Copyright 2008-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.sample.Foo;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcCursorItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {

		JdbcCursorItemReader<Foo> result = new JdbcCursorItemReader<>(getDataSource(),
				"select ID, NAME, VALUE from T_FOOS", new FooRowMapper());
		result.setVerifyCursorPosition(true);

		result.setFetchSize(10);
		result.setMaxRows(100);
		result.setQueryTimeout(1000);
		result.setSaveState(true);
		result.setDriverSupportsAbsolute(false);

		return result;
	}

	@Test
	void testRestartWithDriverSupportsAbsolute() throws Exception {
		tested = getItemReader();
		((JdbcCursorItemReader<Foo>) tested).setDriverSupportsAbsolute(true);
		testedAsStream().open(executionContext);

		testRestart();
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		JdbcCursorItemReader<Foo> reader = (JdbcCursorItemReader<Foo>) tested;
		reader.close();
		reader.setSql("select ID from T_FOOS where ID < 0");
		reader.open(new ExecutionContext());
	}

	@Test
	void testReadBeforeOpen() throws Exception {
		tested = getItemReader();
		assertThrows(ReaderNotOpenException.class, tested::read);
	}

}
