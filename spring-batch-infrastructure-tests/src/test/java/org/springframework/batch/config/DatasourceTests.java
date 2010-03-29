/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.config;

import static org.junit.Assert.*;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/jms/jms-context.xml")
public class DatasourceTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	@BeforeClass
	public static void init() {
		System.setProperty("batch.business.schema.script", "classpath:/org/springframework/batch/jms/init.sql");
	}

	@AfterClass
	public static void cleanup() {
		System.clearProperty("batch.business.schema.script");
	}

	@Transactional @Test
	public void testTemplate() throws Exception {
		System.err.println(System.getProperty("java.class.path"));
		simpleJdbcTemplate.getJdbcOperations().execute("delete from T_BARS");
		int count = simpleJdbcTemplate.queryForInt("select count(*) from T_BARS");
		assertEquals(0, count);

		simpleJdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", 0, "foo");
	}
}
