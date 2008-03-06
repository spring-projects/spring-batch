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
package org.springframework.batch.sample.dao;


import org.springframework.batch.sample.domain.PlayerSummary;
import org.springframework.batch.sample.mapping.PlayerSummaryMapper;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;


/**
 * @author Lucas Ward
 * 
 */
public class JdbcPlayerSummaryDaoIntegrationTests extends
		AbstractTransactionalDataSourceSpringContextTests {

	JdbcPlayerSummaryDao playerSummaryDao;
	PlayerSummary summary;

	protected String[] getConfigLocations() {
		return new String[] { "data-source-context.xml" };
	}

	protected void onSetUpBeforeTransaction() throws Exception {
		super.onSetUpBeforeTransaction();

		playerSummaryDao = new JdbcPlayerSummaryDao();
		playerSummaryDao.setJdbcTemplate(getJdbcTemplate());

		summary = new PlayerSummary();
		summary.setId("AikmTr00");
		summary.setYear(1997);
		summary.setCompletes(294);
		summary.setAttempts(517);
		summary.setPassingYards(3283);
		summary.setPassingTd(19);
		summary.setInterceptions(12);
		summary.setRushes(25);
		summary.setRushYards(79);
		summary.setReceptions(0);
		summary.setReceptionYards(0);
		summary.setTotalTd(0);
	}

	public void testWrite() {

		playerSummaryDao.write(summary);

		PlayerSummary testSummary = (PlayerSummary) getJdbcTemplate()
				.queryForObject("SELECT * FROM PLAYER_SUMMARY",
						new PlayerSummaryMapper());

		assertEquals(testSummary, summary);
	}

}
