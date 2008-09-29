/**
 * 
 */
package org.springframework.batch.sample.domain.trade.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.launch.support.CommandLineJobRunner;
import org.springframework.batch.sample.domain.trade.CustomerUpdate;
import org.springframework.batch.sample.domain.trade.InvalidCustomerLogger;

/**
 * @author Lucas Ward
 *
 */
public class CommonsLoggingInvalidCustomerLogger implements
		InvalidCustomerLogger {
	
	protected static final Log logger = LogFactory.getLog(CommandLineJobRunner.class);

	/* (non-Javadoc)
	 * @see org.springframework.batch.sample.domain.trade.InvalidCustomerLogger#log(org.springframework.batch.sample.domain.trade.CustomerUpdate)
	 */
	public void log(CustomerUpdate customerUpdate) {
		logger.error("invalid customer encountered: [ " + customerUpdate + "]");
	}

}
