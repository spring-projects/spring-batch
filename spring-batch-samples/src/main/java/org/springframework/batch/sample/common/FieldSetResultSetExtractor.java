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

package org.springframework.batch.sample.common;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;

/**
 * ResultSetExtractor implementation that returns list of FieldSets
 * for given ResultSet.
 * 
 * @author peter.zozom
 *
 */
public final class FieldSetResultSetExtractor {
	
	// utility class not meant for instantiation
	private FieldSetResultSetExtractor(){}

	/**
	 * Processes single row in ResultSet and returns its FieldSet representation.
	 * @param rs ResultSet ResultSet to extract data from.
	 * @return FieldSet representation of current row in ResultSet
	 * @throws SQLException thrown during processing
	 */
	public static FieldSet getFieldSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        FieldSet fs;
        
        List<String> tokens = new ArrayList<String>();
        List<String> names = new ArrayList<String>();

        for (int i = 1; i <= columnCount; i++) {
            tokens.add(rs.getString(i));
            names.add(metaData.getColumnName(i));
        }

        fs = new DefaultFieldSet(tokens.toArray(new String[0]), names.toArray(new String[0]));

        return fs;	
	}

}
