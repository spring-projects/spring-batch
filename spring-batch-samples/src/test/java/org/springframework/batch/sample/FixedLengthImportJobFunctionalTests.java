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

package org.springframework.batch.sample;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;

public class FixedLengthImportJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	//expected line length in input file (sum of pattern lengths + 2, because the counter is appended twice)
	private static final int LINE_LENGTH = 29;

	//auto-injected attributes
	private JdbcOperations jdbcTemplate;
	private Resource fileLocator;
	private ItemReader inputSource;

	protected void onSetUp() throws Exception {
		super.onSetUp();
		jdbcTemplate.update("delete from TRADE");
	}

	protected String[] getConfigLocations() {
		return new String[] {"jobs/fixedLengthImportJob.xml"};
	}

	/**
	 * Check that records have been correctly written to database
	 * @throws Exception 
	 */
	protected void validatePostConditions() throws Exception {

		System.err.println(jdbcTemplate.queryForList("SELECT ID, ISIN, QUANTITY, PRICE, CUSTOMER FROM trade ORDER BY id"));

		jdbcTemplate.query("SELECT ID, ISIN, QUANTITY, PRICE, CUSTOMER FROM trade ORDER BY id", new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {
				Trade trade;
				try {
					trade = (Trade)inputSource.read();
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
				assertEquals(trade.getIsin(), rs.getString(2));
				assertEquals(trade.getQuantity(),rs.getLong(3));
				assertEquals(trade.getPrice(), rs.getBigDecimal(4));
				assertEquals(trade.getCustomer(), rs.getString(5));
			}

		});

		System.err.println(inputSource.read());
		assertNull(inputSource.read());
	}

	/*
	 * fixed-length file is expected on input
	 */
	protected void validatePreConditions() throws Exception{
		BufferedReader reader = null;

		reader = new BufferedReader(new FileReader(fileLocator.getFile()));
		String line;
		while ((line = reader.readLine()) != null) {
			assertEquals (LINE_LENGTH, line.length());
		}
	}

	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setFileLocator(Resource fileLocator) {
		this.fileLocator = fileLocator;
	}

	public void setItemReader(ItemReader inputSource){
		this.inputSource = inputSource;
	}
}
