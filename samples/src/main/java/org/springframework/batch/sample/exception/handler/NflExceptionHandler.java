package org.springframework.batch.sample.exception.handler;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.execution.bootstrap.AbstractJobLauncher;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;

public class NflExceptionHandler implements ExceptionHandler {

	private static final Log logger = LogFactory
	.getLog(NflExceptionHandler.class);
	
	public void handleExceptions(RepeatContext context, Collection throwables)
			throws RuntimeException {
		
		Iterator it = throwables.iterator();
		while(it.hasNext()){
			Throwable t = (Throwable)it.next();
			if(!(t instanceof NumberFormatException)){
				throw new RuntimeException(t);
			}
			else{
				logger.error("Number Format Exception!", t);
			}
		}
	}

}
