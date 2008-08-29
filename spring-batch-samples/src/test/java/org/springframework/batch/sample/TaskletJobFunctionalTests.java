package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Deletes files in the given directory.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class TaskletJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private static Resource directory = new FileSystemResource("target/test-outputs/test-dir");
	
	/*
	 * Create the directory and some files in it.
	 */
	@BeforeClass
	public static void onSetUp() throws Exception {
		File dir = directory.getFile();
		dir.mkdirs();
		new File(dir, "file1").createNewFile();
		new File(dir, "file2").createNewFile();
	}

	/**
	 * We have directory with some files in it.
	 */
	@Override
	protected void validatePreConditions() throws Exception {
		assertTrue(directory.getFile().isDirectory());
		assertTrue(directory.getFile().listFiles().length > 0);
	}

	/**
	 * Directory still exists but contains no files.
	 */
	@Override
	protected void validatePostConditions() throws Exception {
		assertTrue(directory.getFile().isDirectory());
		assertEquals(0, directory.getFile().listFiles().length);
	}

}
