/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.resource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.sql.DataSource;

/**
 * @author Lucas Ward
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/core/repository/dao/data-source-context.xml")
public class StepExecutionPreparedStatementSetterTests {

	StepExecutionPreparedStatementSetter pss;

	StepExecution stepExecution;
	
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUpInTransaction() throws Exception {
		
		pss = new StepExecutionPreparedStatementSetter();
		JobParameters jobParameters = new JobParametersBuilder().addLong("begin.id", 1L).addLong("end.id", 4L).toJobParameters();
		JobInstance jobInstance = new JobInstance(1L, jobParameters, "simpleJob");
		JobExecution jobExecution = new JobExecution(jobInstance, 2L);
		stepExecution = new StepExecution("taskletStep", jobExecution, 3L);
		pss.beforeStep(stepExecution);
	}
	
	@Transactional @Test
	public void testSetValues(){
		
		List<String> parameterNames = new ArrayList<String>();
		parameterNames.add("begin.id");
		parameterNames.add("end.id");
		pss.setParameterKeys(parameterNames);
		final List<String> results = new ArrayList<String>();
		simpleJdbcTemplate.getJdbcOperations().query(
				"SELECT NAME from T_FOOS where ID > ? and ID < ?",
				pss,
				new RowCallbackHandler(){
					public void processRow(ResultSet rs) throws SQLException {
						results.add(rs.getString(1));
					}});
		
		assertEquals(2, results.size());
		assertEquals("bar2", results.get(0));
		assertEquals("bar3", results.get(1));
	}
	
	@Transactional @Test
	public void testAfterPropertiesSet() throws Exception{
		try{
			pss.afterPropertiesSet();
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	@Transactional @Test
	public void testNonExistentProperties(){
		
		List<String> parameterNames = new ArrayList<String>();
		parameterNames.add("badParameter");
		parameterNames.add("end.id");
		pss.setParameterKeys(parameterNames);
		
		try{
			simpleJdbcTemplate.getJdbcOperations().query(
					"SELECT NAME from T_FOOS where ID > ? and ID < ?",
					pss,
					new RowCallbackHandler(){
						public void processRow(ResultSet rs) throws SQLException {
							fail();
						}});
			
			fail();
		}catch(IllegalStateException ex){
			//expected
		}

	}
}
