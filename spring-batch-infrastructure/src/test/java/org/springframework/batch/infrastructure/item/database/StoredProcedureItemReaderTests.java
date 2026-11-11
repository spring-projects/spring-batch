/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Hyunwoo Jung
 */
class StoredProcedureItemReaderTests {

	private final Connection connection = mock();

	private final CallableStatement statement = mock();

	private final ResultSet resultSet = mock();

	private StoredProcedureItemReader<String> reader;

	@BeforeEach
	void setUp() throws Exception {
		DataSource dataSource = mock();
		Connection metadataConnection = mock();
		DatabaseMetaData metadata = mock();
		given(dataSource.getConnection()).willReturn(connection, metadataConnection);
		given(connection.getAutoCommit()).willReturn(true);
		given(metadataConnection.getMetaData()).willReturn(metadata);
		given(metadata.getDatabaseProductName()).willReturn("HSQL Database Engine");
		given(connection.prepareCall(anyString(), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY)))
			.willReturn(statement);
		given(statement.getUpdateCount()).willReturn(-1);
		given(statement.getResultSet()).willReturn(resultSet);
		given(resultSet.next()).willReturn(true, false);
		reader = new StoredProcedureItemReader<>(dataSource, "read_foos", (rs, rowNum) -> "foo");
		reader.setVerifyCursorPosition(false);
	}

	@AfterEach
	void tearDown() {
		reader.close();
	}

	@Test
	void readsImmediateResultSet() throws Exception {
		given(statement.execute()).willReturn(true);
		assertReadsResultSet();
		verify(statement, never()).getMoreResults();
		verify(statement, never()).getObject(anyInt());
	}

	@Test
	void findsFirstResultSetAfterMultipleUpdateCounts() throws Exception {
		given(statement.execute()).willReturn(false);
		given(statement.getUpdateCount()).willReturn(0, 2);
		given(statement.getMoreResults()).willReturn(false, true);
		assertReadsResultSet();
		verify(statement, times(2)).getMoreResults();
		verify(statement, never()).getObject(anyInt());
	}

	@Test
	void failsOnOpenWhenThereAreNoResults() throws Exception {
		given(statement.execute()).willReturn(false);
		assertThatExceptionOfType(ItemStreamException.class).isThrownBy(() -> reader.open(new ExecutionContext()))
			.withRootCauseInstanceOf(InvalidDataAccessResourceUsageException.class)
			.withStackTraceContaining("did not return a ResultSet");
		verify(statement, never()).getMoreResults();
		verify(statement, never()).getObject(anyInt());
		verify(statement).close();
		verify(connection).close();
	}

	@Test
	void failsOnOpenWhenThereAreOnlyUpdateCounts() throws Exception {
		given(statement.execute()).willReturn(false);
		given(statement.getUpdateCount()).willReturn(0, 2, -1);
		given(statement.getMoreResults()).willReturn(false);
		assertThatExceptionOfType(ItemStreamException.class).isThrownBy(() -> reader.open(new ExecutionContext()))
			.withRootCauseInstanceOf(InvalidDataAccessResourceUsageException.class)
			.withStackTraceContaining("did not return a ResultSet");
		verify(statement, times(2)).getMoreResults();
		verify(statement, never()).getObject(anyInt());
		verify(statement).close();
		verify(connection).close();
	}

	@Test
	void closesResourcesWhenAdvancingResultsFails() throws Exception {
		SQLException failure = new SQLException("Cannot advance results");
		given(statement.execute()).willReturn(false);
		given(statement.getUpdateCount()).willReturn(0);
		given(statement.getMoreResults()).willThrow(failure);
		assertThatThrownBy(() -> reader.open(new ExecutionContext())).isInstanceOf(ItemStreamException.class)
			.hasRootCause(failure);
		verify(statement).close();
		verify(connection).close();
	}

	@Test
	void readsFunctionOutCursor() throws Exception {
		reader.setFunction(true);
		reader.setParameters(new SqlParameter[] { new SqlOutParameter("cursor", Types.OTHER) });
		given(statement.execute()).willReturn(false);
		given(statement.getObject(1)).willReturn(resultSet);
		assertReadsResultSet();
		verify(statement).registerOutParameter(1, Types.OTHER);
		verify(statement, never()).getMoreResults();
	}

	@Test
	void readsConfiguredOutCursor() throws Exception {
		reader.setRefCursorPosition(2);
		reader.setParameters(new SqlParameter[] { new SqlParameter("id", Types.INTEGER),
				new SqlOutParameter("cursor", Types.REF_CURSOR) });
		given(statement.execute()).willReturn(false);
		given(statement.getObject(2)).willReturn(resultSet);
		assertReadsResultSet();
		verify(statement).registerOutParameter(2, Types.REF_CURSOR);
		verify(statement, never()).getMoreResults();
	}

	@Test
	void prefersImmediateResultSetOverConfiguredOutCursor() throws Exception {
		reader.setRefCursorPosition(1);
		reader.setParameters(new SqlParameter[] { new SqlOutParameter("cursor", Types.REF_CURSOR) });
		given(statement.execute()).willReturn(true);
		assertReadsResultSet();
		verify(statement, never()).getObject(anyInt());
		verify(statement, never()).getMoreResults();
	}

	private void assertReadsResultSet() throws Exception {
		reader.open(new ExecutionContext());
		assertThat(reader.read()).isEqualTo("foo");
		assertThat(reader.read()).isNull();
	}

}
