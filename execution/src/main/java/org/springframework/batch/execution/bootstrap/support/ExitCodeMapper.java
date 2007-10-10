package org.springframework.batch.execution.bootstrap.support;


/**
 * 
 * This interface should be implemented when an environment calling the batch famework has specific
 * requirements regarding the process return codes. 
 *
 * @param The type of returncode expected by the environment
 * @author Stijn Maller
 * @author Lucas Ward
 * @author Dave Syer
 */
public interface ExitCodeMapper {

	static int JVM_EXITCODE_COMPLETED = 0;
	static int JVM_EXITCODE_GENERIC_ERROR = 1;
	static int JVM_EXITCODE_JOB_CONFIGURATION_ERROR = 2;
	public static final String NO_SUCH_JOB_CONFIGURATION = "NO_SUCH_JOB_CONFIGURATION";
	public static final String JOB_CONFIGURATION_NOT_PROVIDED = "JOB_CONFIGURATION_NOT_PROVIDED";

	/**
	 * Transform the exitcode known by the batchframework into an exitcode in the
	 * format of the calling environment.
	 * @param exitCode	The exitcode which is used internally by the batch framework.
	 * @return			The corresponding exitcode as known by the calling environment.
	 */
	public int getExitCode(String exitCode);
	
}
