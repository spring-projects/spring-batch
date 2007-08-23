package org.springframework.batch.execution.repository.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.runtime.JobIdentifier;

/**
 * Shared utility class for {@link JobDao} implementations to allow a
 * {@link JobInstance} to be identified from its {@link JobIdentifier}.
 * 
 * @author Dave Syer
 * 
 */
class JobInstanceFilter {

	/**
	 * Filter the list and pull out a {@link JobInstance} with the supplied
	 * identifier.
	 * 
	 * @param instances
	 *            a collection of {@link JobInstance}
	 * @param identifier
	 *            the required {@link JobIdentifier}
	 * @return another collection of {@link JobInstance}, all matching the the
	 *         given {@link JobIdentifier}
	 */
	public List filter(List instances, JobIdentifier identifier) {
		List result = new ArrayList();
		for (Iterator iterator = instances.iterator(); iterator.hasNext();) {
			JobInstance job = (JobInstance) iterator.next();
			if (job.getIdentifier().equals(identifier)) {
				result.add(job);
			}
		}
		return result;
	}
}
