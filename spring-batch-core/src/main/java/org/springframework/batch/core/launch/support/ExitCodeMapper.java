package org.springframework.batch.core.launch.support;

/**
 * 
 * This interface should be implemented when an environment calling the batch
 * framework has specific requirements regarding the operating system process
 * return status.
 * 
 * @author Stijn Maller
 * @author Lucas Ward
 * @author Dave Syer
 */
public interface ExitCodeMapper {

	static int JVM_EXITCODE_COMPLETED = 0;

	static int JVM_EXITCODE_GENERIC_ERROR = 1;

	static int JVM_EXITCODE_JOB_ERROR = 2;

	public static final String NO_SUCH_JOB = "NO_SUCH_JOB";

	public static final String JOB_NOT_PROVIDED = "JOB_NOT_PROVIDED";

	/**
	 * Convert the exit code from String into an integer that the calling
	 * environment as an operating system can interpret as an exit status.
	 * @param exitCode The exit code which is used internally.
	 * @return The corresponding exit status as known by the calling
	 * environment.
	 */
	public int intValue(String exitCode);

}
