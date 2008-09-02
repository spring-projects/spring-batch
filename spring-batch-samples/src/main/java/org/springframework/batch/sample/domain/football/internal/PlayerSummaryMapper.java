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
package org.springframework.batch.sample.domain.football.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.football.PlayerSummary;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * RowMapper used to map a ResultSet to a {@link PlayerSummary}
 * 
 * @author Lucas Ward
 *
 */
public class PlayerSummaryMapper implements ParameterizedRowMapper<PlayerSummary> {

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.core.RowMapper#mapRow(java.sql.ResultSet, int)
	 */
	public PlayerSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
		
		PlayerSummary summary = new PlayerSummary();
		
		summary.setId(rs.getString(1));
		summary.setYear(rs.getInt(2));
		summary.setCompletes(rs.getInt(3));
		summary.setAttempts(rs.getInt(4));
		summary.setPassingYards(rs.getInt(5));
		summary.setPassingTd(rs.getInt(6));
		summary.setInterceptions(rs.getInt(7));
		summary.setRushes(rs.getInt(8));
		summary.setRushYards(rs.getInt(9));
		summary.setReceptions(rs.getInt(10));
		summary.setReceptionYards(rs.getInt(11));
		summary.setTotalTd(rs.getInt(12));
		
		return summary;
	}

}
