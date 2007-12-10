package org.springframework.batch.core.runtime;

import org.springframework.batch.core.domain.JobIdentifier;


/**
 * Factory for {@link SimpleJobIdentifier} instances.
 * 
 * @author Dave Syer
 *
 */
public class SimpleJobIdentifierFactory implements JobIdentifierFactory {

	/**
	 * Create a {@link JobIdentifier} with the given name.
	 * 
	 * @param name the name for the {@link JobIdentifier}
	 * @return a {@link JobIdentifier} with the given name.
	 * 
	 * @see org.springframework.batch.core.runtime.JobIdentifierFactory#getJobIdentifier(java.lang.String)
	 */
	public JobIdentifier getJobIdentifier(String name) {
		return new SimpleJobIdentifier(name);
	}

}
