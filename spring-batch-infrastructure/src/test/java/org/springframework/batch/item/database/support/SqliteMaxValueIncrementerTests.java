/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.item.database.support;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * @author Luke Taylor
 */
public class SqliteMaxValueIncrementerTests {
	static String dbFile;
	static SimpleDriverDataSource dataSource;
	static JdbcTemplate template;

	@BeforeClass
	public static void setUp() {
		dbFile = System.getProperty("java.io.tmpdir") + File.separator + "batch_sqlite_inc.db";
		dataSource = new SimpleDriverDataSource();
		dataSource.setDriverClass(org.sqlite.JDBC.class);
		dataSource.setUrl("jdbc:sqlite:" + dbFile);
		template = new JdbcTemplate(dataSource);
		template.execute("create table max_value (id integer primary key autoincrement)");
	}

	@AfterClass
	public static void removeDbFile() {
		File db = new File(dbFile);
		if (db.exists()) {
			db.delete();
		}
		dataSource = null;
		template = null;
	}

	@Test
	public void testNextKey() throws Exception {
		SqliteMaxValueIncrementer mvi = new SqliteMaxValueIncrementer(dataSource, "max_value", "id");
		assertEquals(1, mvi.getNextKey());
		assertEquals(2, mvi.getNextKey());
		assertEquals(3, mvi.getNextKey());
		assertEquals(1, template.queryForObject("select count(*) from max_value", Integer.class).intValue());
	}
}
