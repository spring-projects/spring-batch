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

package org.springframework.batch.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * Sample using a step to launch a job.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig
class JobStepFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	private JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	void testJobLaunch() throws Exception {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");

		jobLauncherTestUtils
				.launchJob(new DefaultJobParametersConverter().getJobParameters(PropertiesConverter.stringToProperties(
						"run.id(long)=1,parameter=true,run.date=20070122,input.file=classpath:data/fixedLengthImportJob/input/20070122.teststream.ImportTradeDataStep.txt")));

		int after = JdbcTestUtils.countRowsInTable(jdbcTemplate, "TRADE");
		assertEquals(5, after);
	}

}
