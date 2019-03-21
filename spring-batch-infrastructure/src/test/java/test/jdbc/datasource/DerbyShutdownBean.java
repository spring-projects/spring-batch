/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.jdbc.datasource;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.springframework.beans.factory.DisposableBean;

public class DerbyShutdownBean implements DisposableBean {

	private static Log logger = LogFactory.getLog(DerbyShutdownBean.class);

	private DataSource dataSource;
	
	private boolean isShutdown = false;


	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

    @Override
	public void destroy() throws Exception {
		logger.info("Attempting Derby database shut down on: " + dataSource);
		if (!isShutdown && dataSource != null
				&& dataSource instanceof EmbeddedDataSource) {
			EmbeddedDataSource ds = (EmbeddedDataSource) dataSource;
			try {
				ds.setShutdownDatabase("shutdown");
				ds.getConnection();
			} catch (SQLException except) {
				if (except.getSQLState().equals("08006")) {
					// SQLState derby throws when shutting down the database
					logger.info("Derby database is now shut down.");
					isShutdown = true;
				} else {
					logger.error("Problem shutting down Derby "
							+ except.getMessage());
				}
			}
		}
	}

}
