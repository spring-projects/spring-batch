/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.support;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.infrastructure.item.database.support.SqliteMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.Db2LuwMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.Db2MainframeMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HanaSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SybaseMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MariaDBSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SqlServerSequenceMaxValueIncrementer;

/**
 * @author Lucas Ward
 * @author Will Schipp
 * @author Drummond Dawson
 * @author Mahmoud Ben Hassine
 */
class DefaultDataFieldMaxValueIncrementerFactoryTests {

	private DefaultDataFieldMaxValueIncrementerFactory factory;

	@BeforeEach
	void setUp() {
		DataSource dataSource = mock();
		factory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);
	}

	@Test
	void testSupportedDatabaseType() {
		assertTrue(factory.isSupportedIncrementerType("db2"));
		assertTrue(factory.isSupportedIncrementerType("db2zos"));
		assertTrue(factory.isSupportedIncrementerType("mysql"));
		assertTrue(factory.isSupportedIncrementerType("derby"));
		assertTrue(factory.isSupportedIncrementerType("oracle"));
		assertTrue(factory.isSupportedIncrementerType("postgres"));
		assertTrue(factory.isSupportedIncrementerType("hsql"));
		assertTrue(factory.isSupportedIncrementerType("sqlserver"));
		assertTrue(factory.isSupportedIncrementerType("sybase"));
		assertTrue(factory.isSupportedIncrementerType("sqlite"));
		assertTrue(factory.isSupportedIncrementerType("hana"));
		assertTrue(factory.isSupportedIncrementerType("mariadb"));
	}

	@Test
	void testUnsupportedDatabaseType() {
		assertFalse(factory.isSupportedIncrementerType("invalidtype"));
	}

	@Test
	void testInvalidDatabaseType() {
		assertThrows(IllegalArgumentException.class, () -> factory.getIncrementer("invalidtype", "NAME"));
	}

	@Test
	void testNullIncrementerName() {
		assertThrows(IllegalArgumentException.class, () -> factory.getIncrementer("db2", null));
	}

	@Test
	void testDb2() {
		assertTrue(factory.getIncrementer("db2", "NAME") instanceof Db2LuwMaxValueIncrementer);
	}

	@Test
	void testDb2zos() {
		assertTrue(factory.getIncrementer("db2zos", "NAME") instanceof Db2MainframeMaxValueIncrementer);
	}

	@Test
	void testMysql() {
		assertTrue(factory.getIncrementer("mysql", "NAME") instanceof MySQLMaxValueIncrementer);
	}

	@Test
	void testMariaDB() {
		assertTrue(factory.getIncrementer("mariadb", "NAME") instanceof MariaDBSequenceMaxValueIncrementer);
	}

	@Test
	void testOracle() {
		factory.setIncrementerColumnName("ID");
		assertTrue(factory.getIncrementer("oracle", "NAME") instanceof OracleSequenceMaxValueIncrementer);
	}

	@Test
	void testDerby() {
		assertTrue(factory.getIncrementer("derby", "NAME") instanceof DerbyMaxValueIncrementer);
	}

	@Test
	void testHsql() {
		assertTrue(factory.getIncrementer("hsql", "NAME") instanceof HsqlMaxValueIncrementer);
	}

	@Test
	void testPostgres() {
		assertTrue(factory.getIncrementer("postgres", "NAME") instanceof PostgresSequenceMaxValueIncrementer);
	}

	@Test
	void testMsSqlServer() {
		assertTrue(factory.getIncrementer("sqlserver", "NAME") instanceof SqlServerSequenceMaxValueIncrementer);
	}

	@Test
	void testSybase() {
		assertTrue(factory.getIncrementer("sybase", "NAME") instanceof SybaseMaxValueIncrementer);
	}

	@Test
	void testSqlite() {
		assertTrue(factory.getIncrementer("sqlite", "NAME") instanceof SqliteMaxValueIncrementer);
	}

	@Test
	void testHana() {
		assertTrue(factory.getIncrementer("hana", "NAME") instanceof HanaSequenceMaxValueIncrementer);
	}

}
