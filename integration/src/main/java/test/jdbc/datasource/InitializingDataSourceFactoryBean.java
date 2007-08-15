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

package test.jdbc.datasource;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class InitializingDataSourceFactoryBean extends AbstractFactoryBean {

	private Resource initScript;

	private Resource destroyScript;

	DataSource dataSource;

	public void destroy() throws Exception {
		super.destroy();

		try {
			doExecuteScript(destroyScript);
		}
		catch (Exception e) {
			logger.warn("Could not execute destroy script [" + destroyScript + "]", e);
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource);
		super.afterPropertiesSet();
	}

	protected Object createInstance() throws Exception {
		Assert.notNull(dataSource);
		try {
			doExecuteScript(destroyScript);
		}
		catch (Exception e) {
			logger.debug("Could not execute destroy script [" + destroyScript + "]", e);
		}
		doExecuteScript(initScript);
		return dataSource;
	}

	private void doExecuteScript(final Resource scriptResource) {
		if (scriptResource == null || !scriptResource.exists())
			return;
		TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
		if (initScript != null) {
			transactionTemplate.execute(new TransactionCallback() {

				public Object doInTransaction(TransactionStatus status) {
					JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
					String[] scripts;
					try {
						scripts = StringUtils.delimitedListToStringArray(stripComments(IOUtils.readLines(scriptResource
								.getInputStream())), ";");
					}
					catch (IOException e) {
						throw new BeanInitializationException("Cannot load script from [" + initScript + "]", e);
					}
					for (int i = 0; i < scripts.length; i++) {
						String script = scripts[i].trim();
						if (StringUtils.hasText(script)) {
							jdbcTemplate.execute(scripts[i]);
						}
					}
					return null;
				}

			});

		}
	}

	private String stripComments(List list) {
		StringBuffer buffer = new StringBuffer();
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			String line = (String) iter.next();
			if (!line.startsWith("//") && !line.startsWith("--")) {
				buffer.append(line + "\n");
			}
		}
		return buffer.toString();
	}

	public Class getObjectType() {
		return DataSource.class;
	}

	public void setInitScript(Resource initScript) {
		this.initScript = initScript;
	}

	public void setDestroyScript(Resource destroyScript) {
		this.destroyScript = destroyScript;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

}
