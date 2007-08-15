package test.jdbc.datasource;

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

public class DerbyDataSourceFactoryBean extends AbstractFactoryBean {

	private String dataDirectory = "derby-home";

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

	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}

	protected Object createInstance() throws Exception {
		File directory = new File(dataDirectory);
		System.setProperty("derby.system.home", directory.getCanonicalPath());
		System.setProperty("derby.storage.fileSyncTransactionLog", "true");
		System.setProperty("derby.storage.pageCacheSize", "100");

		final EmbeddedDataSource ds = new EmbeddedDataSource();
		ds.setDatabaseName("derbydb");
		ds.setCreateDatabase("create");
		dataSource = ds;

		try {
			doExecuteScript(destroyScript);
		}
		catch (Exception e) {
			logger.debug("Could not execute destroy script [" + destroyScript + "]", e);
		}
		doExecuteScript(initScript);
		return ds;
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
						scripts = StringUtils.delimitedListToStringArray(IOUtils.toString(scriptResource
								.getInputStream()), ";");
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

	public Class getObjectType() {
		return DataSource.class;
	}

	public void setInitScript(Resource initScript) {
		this.initScript = initScript;
	}

	public void setDestroyScript(Resource destroyScript) {
		this.destroyScript = destroyScript;
	}

}
