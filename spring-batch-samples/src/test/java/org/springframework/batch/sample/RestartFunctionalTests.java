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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Simple restart scenario.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class RestartFunctionalTests extends AbstractBatchLauncherTests {

	// auto-injected attributes
	private JdbcOperations jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}


	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractSingleSpringContextTests#onTearDown()
	 */
	@BeforeTransaction
	public void onTearDown() throws Exception {
		jdbcTemplate.update("DELETE FROM TRADE");
	}

	/**
	 * Job fails on first run, because the module throws exception after
	 * processing more than half of the input. On the second run, the job should
	 * finish successfully, because it continues execution where the previous
	 * run stopped (module throws exception after fixed number of processed
	 * records).
	 * @throws Exception the exception thrown
	 */
	@Test
	public void testRestart() throws Exception {

		int before = jdbcTemplate.queryForInt("SELECT COUNT(*) FROM TRADE");

		try {
			runJob();
			fail("First run of the job is expected to fail.");
		}
		catch (UnexpectedJobExecutionException expected) {
			// expected
			assertTrue("Not planned exception: " + expected.getMessage(), expected.getMessage().toLowerCase().indexOf(
					"planned") >= 0);
		}

		int medium = jdbcTemplate.queryForInt("SELECT COUNT(*) FROM TRADE");
		// assert based on commit interval = 2
		assertEquals(before + 2, medium);

		runJob();

		int after = jdbcTemplate.queryForInt("SELECT COUNT(*) FROM TRADE");

		assertEquals(before + 5, after);
	}

	// load the application context and launch the job
	private void runJob() throws Exception {
		launcher.run(getJob(), new DefaultJobParametersConverter().getJobParameters(PropertiesConverter
				.stringToProperties("restart=true")));
	}

}
