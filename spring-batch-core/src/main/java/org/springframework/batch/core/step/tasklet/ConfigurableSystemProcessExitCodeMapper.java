package org.springframework.batch.core.step.tasklet;

import java.util.Map;

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.util.Assert;

/**
 * Maps exit codes to {@link org.springframework.batch.repeat.ExitStatus} 
 * according to injected map. The injected map is required to contain a value 
 * for 'else' key, this value will be returned if the injected map 
 * does not contain value for the exit code returned by the system process.
 * 
 * @author Robert Kasanicky
 */
public class ConfigurableSystemProcessExitCodeMapper implements SystemProcessExitCodeMapper {
	
	public static final String ELSE_KEY = "else";
	
	private Map<Object, ExitStatus> mappings;

	public ExitStatus getExitStatus(int exitCode) {
		ExitStatus exitStatus = mappings.get(exitCode);
		if (exitStatus != null) {
			return exitStatus;
		} else {
			return mappings.get(ELSE_KEY);
		}
	}

	/**
	 * @param mappings <code>Integer</code> exit code keys to 
	 * {@link org.springframework.batch.repeat.ExitStatus} values.
	 */
	public void setMappings(Map<Object, ExitStatus> mappings) {
		Assert.notNull(mappings.get(ELSE_KEY));
		this.mappings = mappings;
	}

}
