/*
 * Copyright 2010-2018 the original author or authors.
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

import java.io.File;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class DerbyDataSourceFactoryBean extends AbstractFactoryBean<DataSource> {

	private static Log logger = LogFactory.getLog(DerbyDataSourceFactoryBean.class);

	private String dataDirectory = "build/derby-home";

	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

    @Override
	protected DataSource createInstance() throws Exception {
		File directory = new File(dataDirectory);
		System.setProperty("derby.system.home", directory.getCanonicalPath());
		System.setProperty("derby.storage.fileSyncTransactionLog", "true");
		System.setProperty("derby.storage.pageCacheSize", "100");

		final EmbeddedDataSource ds = new EmbeddedDataSource();
		ds.setDatabaseName("build/derbydb");
		ds.setCreateDatabase("create");

		logger.info("Created instance of " + ds.toString());
		
		return ds;
	}

    @Override
	public Class<DataSource> getObjectType() {
		return DataSource.class;
	}

}
