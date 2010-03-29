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
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class DataSourceInitializer implements InitializingBean, DisposableBean {

	private Resource[] initScripts;

	private Resource destroyScript;

	private DataSource dataSource;

	private boolean initialize = false;

	private Log logger = LogFactory.getLog(getClass());

	private boolean initialized = false;

	public void setInitialize(boolean initialize) {
		this.initialize = initialize;
	}

	public void destroy() throws Exception {
		if (!initialized) {
			return;
		}
		try {
			if (destroyScript!=null) {
				doExecuteScript(destroyScript);
				initialized = false;
			}
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.warn("Could not execute destroy script [" + destroyScript + "]", e);
			}
			else {
				logger.warn("Could not execute destroy script [" + destroyScript + "]");
			}
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(dataSource);
        logger.info("Initializing with scripts: "+Arrays.asList(initScripts));
		if (!initialized && initialize) {
			try {
				doExecuteScript(destroyScript);
			}
			catch (Exception e) {
				logger.debug("Could not execute destroy script [" + destroyScript + "]", e);
			}
			if (initScripts != null) {
				for (int i = 0; i < initScripts.length; i++) {
					Resource initScript = initScripts[i];
                    logger.info("Executing init script: "+initScript);
					doExecuteScript(initScript);
				}
			}
			initialized = true;
		}
	}

	private void doExecuteScript(final Resource scriptResource) {
		if (scriptResource == null || !scriptResource.exists())
			return;
		TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
		transactionTemplate.execute(new TransactionCallback() {

			@SuppressWarnings("unchecked")
			public Object doInTransaction(TransactionStatus status) {
				JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
				String[] scripts;
				try {
					scripts = StringUtils.delimitedListToStringArray(stripComments(IOUtils.readLines(scriptResource
							.getInputStream())), ";");
				}
				catch (IOException e) {
					throw new BeanInitializationException("Cannot load script from [" + scriptResource + "]", e);
				}
				for (int i = 0; i < scripts.length; i++) {
					String script = scripts[i].trim();
					if (StringUtils.hasText(script)) {
						try {
							jdbcTemplate.execute(scripts[i]);
						} catch (DataAccessException e) {
							if (!script.toUpperCase().startsWith("DROP")) {
								throw e;
							}
						}
					}
				}
				return null;
			}

		});

	}

	private String stripComments(List<String> list) {
		StringBuffer buffer = new StringBuffer();
		for (String line : list) {
			if (!line.startsWith("//") && !line.startsWith("--")) {
				buffer.append(line + "\n");
			}
		}
		return buffer.toString();
	}

	public Class<DataSource> getObjectType() {
		return DataSource.class;
	}

	public void setInitScripts(Resource[] initScripts) {
		this.initScripts = initScripts;
	}

	public void setDestroyScript(Resource destroyScript) {
		this.destroyScript = destroyScript;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

}
