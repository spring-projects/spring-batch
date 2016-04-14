/*
 * Copyright 2016 the original author or authors.
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

/**
 * <p>
 * Extensions of the {@link AbstractNeo4jItemReader} intended for use with versions of
 * Spring Data Neo4J &gt; 4.  Conversions of the results are done using logic based on the
 * target type.
 * </p>
 *
 * @author Michael Minella
 * @author Vince Bickers
 * @since 3.0.7
 */
public class Neo4j4ItemReader<T> extends AbstractNeo4jItemReader {

	@Override
	protected Iterator<T> doPageRead() {
		Iterable queryResults = getTemplate().queryForObjects(
				getTargetType(), generateLimitCypherQuery(), getParameterValues());

		if(queryResults != null) {
			return queryResults.iterator();
		}
		else {
			return new ArrayList<T>().iterator();
		}
	}
}
