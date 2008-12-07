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

package org.springframework.batch.item.database.support;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import javax.sql.DataSource;

/**
 * Derby implementation of a  {@link PagingQueryProvider} using standard SQL:2003 windowing functions.
 * These features are supported starting with Apache Derby version 10.4.1.3.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class DerbyPagingQueryProvider extends SqlWindowingPagingQueryProvider {

	@Override
	public void init(DataSource dataSource) throws Exception {
		super.init(dataSource);
		String version = JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductVersion").toString();
		if ("10.4.1.3".compareTo(version) > 0) {
			throw new InvalidDataAccessResourceUsageException("Apache Derby version " + version + " is not supported by this class,  Only version 10.4.1.3 or later is supported");
		}
	}
}
