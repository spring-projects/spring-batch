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
package org.springframework.batch.sample.domain.football.internal;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.football.PlayerSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lucas Ward
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/data-source-context.xml" })
public class JdbcPlayerSummaryDaoIntegrationTests {

	private JdbcPlayerSummaryDao playerSummaryDao;

	private PlayerSummary summary;

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void init(DataSource dataSource) {

		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
		playerSummaryDao = new JdbcPlayerSummaryDao();
		playerSummaryDao.setDataSource(dataSource);

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

	@Before
	public void onSetUpInTransaction() throws Exception {

		simpleJdbcTemplate.getJdbcOperations().execute("delete from PLAYER_SUMMARY");

	}

	@Transactional
	@Test
	public void testWrite() {

		playerSummaryDao.write(Collections.singletonList(summary));

		PlayerSummary testSummary = simpleJdbcTemplate.queryForObject("SELECT * FROM PLAYER_SUMMARY",
				new PlayerSummaryMapper());

		assertEquals(summary, testSummary);

	}

}
