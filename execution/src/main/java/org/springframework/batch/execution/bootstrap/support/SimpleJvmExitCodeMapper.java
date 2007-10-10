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

package org.springframework.batch.execution.bootstrap.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.repeat.ExitStatus;


/**
 * An implementation of {@link ExitCodeMapper} that can be configured 
 * through a map from batch exit codes (String) to integer results.
 * 
 * @author Stijn Maller
 * @author Lucas Ward
 * @author Dave Syer
 */

public class SimpleJvmExitCodeMapper implements ExitCodeMapper {

	protected Log logger = LogFactory.getLog(getClass());
	
	private Map mapping;
	
	public SimpleJvmExitCodeMapper(){
		mapping = new HashMap();
		mapping.put(ExitStatus.FINISHED.getExitCode(), 
				new Integer(JVM_EXITCODE_COMPLETED));
		mapping.put(ExitStatus.FAILED.getExitCode(), 
				new Integer(JVM_EXITCODE_GENERIC_ERROR));
		mapping.put(ExitCodeMapper.JOB_CONFIGURATION_NOT_PROVIDED, 
				new Integer(JVM_EXITCODE_JOB_CONFIGURATION_ERROR));
		mapping.put(ExitCodeMapper.NO_SUCH_JOB_CONFIGURATION, 
				new Integer(JVM_EXITCODE_JOB_CONFIGURATION_ERROR));
	}

	public Map getMapping() {
		return mapping;
	}

	/**
	 * Supply the ExitCodeMappings 
	 * @param exitCodeMap A set of mappings between environment specific exit codes
	 * 					  and batch framework internal exit codes
	 */
	public void setMapping(Map exitCodeMap) {
		mapping.putAll(exitCodeMap);
	}

	/**
	 * Get the JVM exitcode that matches a certain Batch Framework Exitcode
	 * @param exitCode The exitcode of the Batch Job as known by the Batch Framework
	 * @return The exitCode of the Batch Job as known by the JVM 
	 */
	public int getExitCode(String exitCode) {
		
		Integer statusCode = null;
		
		try{
			statusCode = (Integer)mapping.get(exitCode);
		}
		catch(RuntimeException ex){ 
			//We still need to return an exit code, even if there is an issue with
			//the mapper.
			logger.fatal("Error mapping exit code, generic exit code returned.", ex);
		}
		
		return (statusCode != null) ? statusCode.intValue() : JVM_EXITCODE_GENERIC_ERROR;
	}
	
}
