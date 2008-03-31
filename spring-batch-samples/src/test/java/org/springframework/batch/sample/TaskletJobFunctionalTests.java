package org.springframework.batch.sample;

import java.io.File;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Deletes files in the given directory.
 * 
 * @author Robert Kasanicky
 */
public class TaskletJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private Resource directory;
	
	/**
	 * Setter for auto-injection.
	 */ 
	public void setDirectory(Resource directory) {
		this.directory = directory;
	}

	/**
	 * Create the directory and some files in it.
	 */
	protected void onSetUp() throws Exception {
		File dir = directory.getFile();
		dir.mkdirs();
		new File(dir, "file1").createNewFile();
		new File(dir, "file2").createNewFile();
	}

	/**
	 * We have directory with some files in it.
	 */
	protected void validatePreConditions() throws Exception {
		Assert.state(directory.getFile().isDirectory());
		Assert.state(directory.getFile().listFiles().length > 0);
	}

	/**
	 * Directory still exists but contains no files.
	 */
	protected void validatePostConditions() throws Exception {
		Assert.state(directory.getFile().isDirectory());
		Assert.state(directory.getFile().listFiles().length == 0);
	}

}
