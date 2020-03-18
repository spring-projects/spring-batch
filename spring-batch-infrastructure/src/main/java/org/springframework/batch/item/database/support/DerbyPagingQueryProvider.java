/*
 * Copyright 2006-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.database.support;

import javax.sql.DataSource;

import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * Derby implementation of a  {@link PagingQueryProvider} using standard SQL:2003 windowing functions.
 * These features are supported starting with Apache Derby version 10.4.1.3.
 *
 * As the OVER() function does not support the ORDER BY clause a sub query is instead used to order the results
 * before the ROW_NUM restriction is applied
 *
 * @author Thomas Risberg
 * @author David Thexton
 * @author Michael Minella
 * @since 2.0
 */
public class DerbyPagingQueryProvider extends SqlWindowingPagingQueryProvider {
	
	private static final String MINIMAL_DERBY_VERSION = "10.4.1.3";

	@Override
	public void init(DataSource dataSource) throws Exception {
		super.init(dataSource);
		String version = JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductVersion").toString();
		if (!isDerbyVersionSupported(version)) {
			throw new InvalidDataAccessResourceUsageException("Apache Derby version " + version + " is not supported by this class,  Only version " + MINIMAL_DERBY_VERSION + " or later is supported");
		}
	}
	
	// derby version numbering is M.m.f.p [ {alpha|beta} ] see https://db.apache.org/derby/papers/versionupgrade.html#Basic+Numbering+Scheme
	private boolean isDerbyVersionSupported(String version) {
		String[] minimalVersionParts = MINIMAL_DERBY_VERSION.split("\\.");
		String[] versionParts = version.split("[\\. ]");
		for (int i = 0; i < minimalVersionParts.length; i++) {
			int minimalVersionPart = Integer.valueOf(minimalVersionParts[i]);
			int versionPart = Integer.valueOf(versionParts[i]);
			if (versionPart < minimalVersionPart) {
				return false;
			} else if (versionPart > minimalVersionPart) {
				return true;
			}
		}
		return true; 
	}
	
	@Override
	protected String getOrderedQueryAlias() {
		return "TMP_ORDERED";
	}

	@Override
	protected String getOverClause() {
		return "";
	}

    @Override
	protected String getOverSubstituteClauseStart() {
		return " FROM (SELECT " + getSelectClause();
	}

    @Override
	protected String getOverSubstituteClauseEnd() {
		return " ) AS " + getOrderedQueryAlias();
	}

}
