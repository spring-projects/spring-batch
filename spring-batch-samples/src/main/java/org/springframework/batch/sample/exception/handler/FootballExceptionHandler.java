package org.springframework.batch.sample.exception.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;

public class FootballExceptionHandler implements ExceptionHandler {

	private static final Log logger = LogFactory
			.getLog(FootballExceptionHandler.class);

	public void handleException(RepeatContext context, Throwable throwable)
			throws RuntimeException {

		if (!(throwable instanceof NumberFormatException)) {
			throw new RuntimeException(throwable);
		} else {
			logger.error("Number Format Exception!", throwable);
		}

	}

}
