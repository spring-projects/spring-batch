/*
 * Copyright 2008-2014 the original author or authors.
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

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;

@RunWith(JUnit4.class)
@SuppressWarnings("deprecation")
public class IbatisPagingItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader<Foo> reader = new IbatisPagingItemReader<Foo>();
		reader.setQueryId("getPagedFoos");
		reader.setPageSize(2);
		reader.setSqlMapClient(sqlMapClient);
		reader.setDataSource(getDataSource());
		reader.setSaveState(true);

		reader.afterPropertiesSet();

		return reader;
	}

	private SqlMapClient createSqlMapClient() throws Exception {
		return SqlMapClientBuilder.buildSqlMapClient(new ClassPathResource("ibatis-config.xml", getClass()).getInputStream());
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		IbatisPagingItemReader<Foo> reader = (IbatisPagingItemReader<Foo>) tested;
		reader.close();

		reader.setQueryId("getNoFoos");
		reader.afterPropertiesSet();

		reader.open(new ExecutionContext());
	}

}
