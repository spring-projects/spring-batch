/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.execution.configuration;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public class StubDataSource implements DataSource {

	public Connection getConnection() throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

	public Connection getConnection(String username, String password) throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

	public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

	public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

	public void setLogWriter(PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

	public void setLoginTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }
	
}