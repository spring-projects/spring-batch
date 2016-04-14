/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.batch.item.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.util.ClassUtils;

/**
 * <p>
 * Extensions of the {@link AbstractNeo4jItemReader} intended for use with versions of
 * Spring Data Neo4J &lt; 4.  Conversions of the results are done using an external
 * {@link ResultConverter}.
 * </p>
 *
 * @author Michael Minella
 * @see org.springframework.batch.item.data.Neo4j4ItemReader
 */
public class Neo4jItemReader<T> extends AbstractNeo4jItemReader {

	protected Log logger = LogFactory.getLog(getClass());

	private ResultConverter<Map<String, Object>, T> resultConverter;

	public Neo4jItemReader() {
		setName(ClassUtils.getShortName(Neo4jItemReader.class));
	}

	/**
	 * Set the converter used to convert node to the targetType.  By
	 * default, {@link DefaultConverter} is used.
	 *
	 * @param resultConverter the converter to use.
	 */
	public void setResultConverter(ResultConverter<Map<String, Object>, T> resultConverter) {
		this.resultConverter = resultConverter;
	}

	@Override
	protected Iterator<T> doPageRead() {
		Result<Map<String, Object>> queryResults = getTemplate().query(
				generateLimitCypherQuery(), getParameterValues());

		if(queryResults != null) {
			if (resultConverter != null) {
				return queryResults.to(getTargetType(), resultConverter).iterator();
			}
			else {
				return queryResults.to(getTargetType()).iterator();
			}
		}
		else {
			return new ArrayList<T>().iterator();
		}
	}
}
