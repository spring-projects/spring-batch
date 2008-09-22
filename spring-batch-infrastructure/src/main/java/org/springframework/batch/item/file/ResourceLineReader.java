package org.springframework.batch.item.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.separator.SimpleRecordSeparatorPolicy;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Restartable {@link ItemReader} that reads lines from input
 * {@link #setResource(Resource)}. Line is defined by the
 * {@link #setRecordSeparatorPolicy(RecordSeparatorPolicy)}.
 * 
 * @author Robert Kasanicky
 */
public class ResourceLineReader extends AbstractItemCountingItemStreamItemReader<String> implements
		ResourceAwareItemReaderItemStream<String> {

	private static final Log logger = LogFactory.getLog(ResourceLineReader.class);

	// default encoding for input files
	public static final String DEFAULT_CHARSET = "ISO-8859-1";

	private RecordSeparatorPolicy recordSeparatorPolicy = new SimpleRecordSeparatorPolicy();

	private Resource resource;

	private BufferedReader reader;

	private int lineCount = 0;

	private String[] comments = new String[] { "#" };

	private boolean noInput = false;

	private String encoding = DEFAULT_CHARSET;
	
	public ResourceLineReader() {
		setName(ClassUtils.getShortName(FlatFileItemReader.class));
	}

	/**
	 * Setter for the encoding for this input source. Default value is
	 * {@link #DEFAULT_CHARSET}.
	 * 
	 * @param encoding a properties object which possibly contains the encoding
	 * for this input file;
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Setter for comment prefixes. Can be used to ignore header lines as well
	 * by using e.g. the first couple of column names as a prefix.
	 * 
	 * @param comments an array of comment line prefixes.
	 */
	public void setComments(String[] comments) {
		this.comments = new String[comments.length];
		System.arraycopy(comments, 0, this.comments, 0, comments.length);
	}

	/**
	 * Public setter for the input resource.
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Public setter for the recordSeparatorPolicy. Used to determine where the
	 * line endings are and do things like continue over a line ending if inside
	 * a quoted string.
	 * 
	 * @param recordSeparatorPolicy the recordSeparatorPolicy to set
	 */
	public void setRecordSeparatorPolicy(RecordSeparatorPolicy recordSeparatorPolicy) {
		this.recordSeparatorPolicy = recordSeparatorPolicy;
	}

	/**
	 * @return string corresponding to logical record according to
	 * {@link #setRecordSeparatorPolicy(RecordSeparatorPolicy)} (might span
	 * multiple lines in file).
	 */
	@Override
	protected String doRead() {
		if (noInput) {
			return null;
		}
		String line = readLine();
		String record = line;
		if (line != null) {
			while (line != null && !recordSeparatorPolicy.isEndOfRecord(record)) {
				record = recordSeparatorPolicy.preProcess(record) + (line = readLine());
			}
		}
		return recordSeparatorPolicy.postProcess(record);
	}

	/**
	 * @return next line (skip comments).
	 */
	private String readLine() {

		if (reader == null) {
			throw new ReaderNotOpenException("Reader must be open before it can be read.");
		}

		String line = null;

		try {
			line = this.reader.readLine();
			if (line == null) {
				return null;
			}
			lineCount++;
			while (isComment(line)) {
				line = reader.readLine();
				if (line == null) {
					return null;
				}
				lineCount++;
			}
		}
		catch (IOException e) {
			throw new UnexpectedInputException("Unable to read from resource '" + resource + "' at line " + lineCount,
					e);
		}
		return line;
	}

	private boolean isComment(String line) {
		for (String prefix : comments) {
			if (line.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void doClose() throws Exception {
		lineCount = 0;
		reader.close();

	}

	@Override
	protected void doOpen() throws Exception {
		Assert.notNull(resource, "Input resource must be set");
		Assert.notNull(recordSeparatorPolicy, "RecordSeparatorPolicy must be set");

		noInput = false;
		if (!resource.exists()) {
			noInput = true;
			logger.warn("Input resource does not exist");
			return;
		}

		reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), encoding));

	}

}
