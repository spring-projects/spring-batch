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

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;

public class FixedLengthImportJobFunctionalTests extends AbstractLifecycleSpringContextTests {
	
	//expected line length in input file (sum of pattern lengths + 2, because the counter is appended twice)
	private static final int LINE_LENGTH = 29;

	//auto-injected attributes
	private JdbcOperations jdbcTemplate;
	private Resource fileLocator;
	private FieldSetInputSource inputSource; 
	
	protected void onSetUp() throws Exception {
		super.onSetUp();
		jdbcTemplate.update("delete from TRADE");
	}

	protected String[] getConfigLocations() {
		return new String[] {"jobs/fixedLengthImportJob.xml"};
	}

	/**
	 * check that records have been correctly written to database
	 */
	protected void validatePostConditions() {
		
		((ResourceLifecycle) inputSource).open();
		
		jdbcTemplate.query("SELECT ID, ISIN, QUANTITY, PRICE, CUSTOMER FROM trade ORDER BY id", new RowCallbackHandler() {
			
			public void processRow(ResultSet rs) throws SQLException {
				FieldSet fieldSet = inputSource.readFieldSet();
				assertEquals(fieldSet.readString(0), rs.getString(2));
				assertEquals(fieldSet.readLong(1),rs.getLong(3));
				assertEquals(fieldSet.readBigDecimal(2), rs.getBigDecimal(4));
				assertEquals(fieldSet.readString(3), rs.getString(5));
			}
			
		});
		
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
	
	public void setFieldSetInputSource(FieldSetInputSource inputSource){
		this.inputSource = inputSource;
	}
}
