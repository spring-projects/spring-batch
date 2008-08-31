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
package org.springframework.batch.item.database;

import org.springframework.batch.item.database.support.IbatisKeyCollector;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Extension of {@link DrivingQueryItemReader} that maps keys to objects. An
 * iBatis query id must be set to map and return each 'detail record'.
 * 
 * The writer is thread safe after its properties are set (normal singleton
 * behaviour).
 * 
 * TODO remove this class? It does not return keys so it shouldn't extend 
 * DrivingQueryItemReader
 * 
 * @author Lucas Ward
 * @see IbatisKeyCollector
 * @deprecated The DrivingQueryItemReader approach is not supported going forward, use a PagingItemReader
 * implementation instead.  See {@link org.springframework.batch.item.database.AbstractPagingItemReader}
 */
@SuppressWarnings("unchecked")
public class IbatisDrivingQueryItemReader extends DrivingQueryItemReader {

	private String detailsQueryId;

	private SqlMapClientTemplate sqlMapClientTemplate;

	/**
	 * Overridden read() that uses the returned key as arguments to the details
	 * query.
	 * 
	 * @see org.springframework.batch.item.database.DrivingQueryItemReader#read()
	 */
	public Object read() {
		Object key = super.read();
		if (key == null) {
			return null;
		}
		return sqlMapClientTemplate.queryForObject(detailsQueryId, key);
	}

	/**
	 * @param detailsQueryId id of the iBATIS select statement that will used to
	 * retrieve an object for a single primary key from the list returned by
	 * driving query
	 */
	public void setDetailsQueryId(String detailsQueryId) {
		this.detailsQueryId = detailsQueryId;
	}

	/**
	 * Set the {@link SqlMapClientTemplate} to use for this input source.
	 * 
	 * @param sqlMapClient
	 */
	public void setSqlMapClient(SqlMapClient sqlMapClient) {
		this.sqlMapClientTemplate = new SqlMapClientTemplate(sqlMapClient);
	}
}
