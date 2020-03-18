/*
 * Copyright 2008-2012 the original author or authors.
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

import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for {@link JdbcCursorItemReader}
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcCursorItemReaderIntegrationTests extends AbstractGenericDataSourceItemReaderIntegrationTests {

    @Override
	protected ItemReader<Foo> createItemReader() throws Exception {
		JdbcCursorItemReader<Foo> result = new JdbcCursorItemReader<>();
		result.setDataSource(dataSource);
		result.setSql("select ID, NAME, VALUE from T_FOOS");
		result.setIgnoreWarnings(true);
		result.setVerifyCursorPosition(true);
		
		result.setRowMapper(new FooRowMapper());
		result.setFetchSize(10);
		result.setMaxRows(100);
		result.setQueryTimeout(1000);
		result.setSaveState(true);

		return result;
	}

	
	
}
