package org.springframework.batch.sample.tasklet;

import java.io.File;

import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Deletes files in given directory. Ignores subdirectories. Fails (by throwing
 * exception) if any of the files could not be deleted.
 * 
 * @author Robert Kasanicky
 */
public class FileDeletingTasklet implements Tasklet, InitializingBean {

	private Resource directory;

	public ExitStatus execute() throws Exception {
		File dir = directory.getFile();
		Assert.state(dir.isDirectory());

		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			boolean deleted = files[i].delete();
			if (!deleted) {
				throw new UnexpectedJobExecutionException("Could not delete file " + files[i].getPath());
			}
		}
		return ExitStatus.FINISHED;
	}

	public void setDirectoryResource(Resource directory) {
		this.directory = directory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(directory, "directory must be set");
	}

}
