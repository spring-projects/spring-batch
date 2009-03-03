/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Helper methods for SQL statement parameter parsing.
 *
 * Only intended for internal use.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 */
public class JdbcParameterUtils {

	/**
	 * Count the occurrences of the character placeholder in an SQL string
	 * <code>sql</code>. The character placeholder is not counted if it appears
	 * within a literal, that is, surrounded by single or double quotes. This method will
	 * count traditional placeholders in the form of a question mark ('?') as well as
	 * named parameters indicated with a leading ':' or '&'.
	 *
	 * The code for this method is taken from an early version of the
	 * {@link org.springframework.jdbc.core.namedparam.NamedParameterUtils}
	 * class. That method was later removed after some refactoring, but the code
	 * is useful here for the Spring Batch project. The code has been altered to better
	 * suite the batch processing requirements.
	 *
	 * @param sql String to search in. Returns 0 if the given String is <code>null</code>.
	 */
	public static int countParameterPlaceholders(String sql, List<String> namedParameterHolder ) {
		if (sql == null) {
			return 0;
		}

		char[] statement = sql.toCharArray();
		boolean withinQuotes = false;
		Map<String, StringBuilder> namedParameters = new HashMap<String, StringBuilder>();
		char currentQuote = '-';
		int parameterCount = 0;
		int i = 0;
		while (i < statement.length) {
			if (withinQuotes) {
				if (statement[i] == currentQuote) {
					withinQuotes = false;
					currentQuote = '-';
				}
			}
			else {
				if (statement[i] == '"' || statement[i] == '\'') {
					withinQuotes = true;
					currentQuote = statement[i];
				}
				else {
					if (statement[i] == ':' || statement[i] == '&') {
						int j = i + 1;
						StringBuilder parameter = new StringBuilder();
						while (j < statement.length && parameterNameContinues(statement, j)) {
							parameter.append(statement[j]);
							j++;
						}
						if (j - i > 1) {
							if (!namedParameters.containsKey(parameter.toString())) {
								parameterCount++;
								namedParameters.put(parameter.toString(), parameter);
								i = j - 1;
							}
						}
					}
					else {
						if (statement[i] == '?') {
							parameterCount++;
						}
					}
				}
			}
			i++;
		}
		if (namedParameterHolder != null) {
			namedParameterHolder.addAll(namedParameters.keySet());
		}
		return parameterCount;
	}

	/**
	 * Determine whether a parameter name continues at the current position,
	 * that is, does not end delimited by any whitespace character yet.
	 * @param statement the SQL statement
	 * @param pos the position within the statement
	 */
	private static boolean parameterNameContinues(char[] statement, int pos) {
		return (statement[pos] != ' ' && statement[pos] != ',' && statement[pos] != ')' &&
				statement[pos] != '"' && statement[pos] != '\'' && statement[pos] != '|' &&
				statement[pos] != ';' && statement[pos] != '\n' && statement[pos] != '\r');
	}

}
