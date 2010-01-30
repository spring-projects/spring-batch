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
