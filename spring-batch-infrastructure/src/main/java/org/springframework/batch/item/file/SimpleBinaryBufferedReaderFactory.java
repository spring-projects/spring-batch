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
package org.springframework.batch.item.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.springframework.core.io.Resource;

/**
 * A {@link BufferedReaderFactory} useful for reading simple binary (or text)
 * files with no line endings, such as those produced by mainframe copy books.
 * The reader splits a stream up across fixed line endings (rather than the
 * usual convention based on plain text). The line endings are discarded, just
 * as with the default plain text implementation.
 * 
 * @author Dave Syer
 * 
 * @since 2.1
 */
public class SimpleBinaryBufferedReaderFactory implements BufferedReaderFactory {

	/**
	 * The default line ending value.
	 */
	private static final String DEFAULT_LINE_ENDING = "\n";

	private String lineEnding = DEFAULT_LINE_ENDING;

	/**
	 * @param lineEnding
	 */
	public void setLineEnding(String lineEnding) {
		this.lineEnding = lineEnding;
	}

	public BufferedReader create(Resource resource, String encoding) throws UnsupportedEncodingException, IOException {
		return new BinaryBufferedReader(new InputStreamReader(resource.getInputStream(), encoding), lineEnding);
	}

	/**
	 * BufferedReader extension that splits lines based on a line ending, rather
	 * than the usual plain text conventions.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private final class BinaryBufferedReader extends BufferedReader {

		private final String ending;

		/**
		 * @param in
		 */
		private BinaryBufferedReader(Reader in, String ending) {
			super(in);
			this.ending = ending;
		}

		@Override
		public String readLine() throws IOException {

			StringBuilder buffer = null;

			synchronized (lock) {

				int next = read();
				if (next == -1) {
					return null;
				}

				buffer = new StringBuilder();
				StringBuilder candidateEnding = new StringBuilder();

				while (!isEndOfLine(buffer, candidateEnding, next)) {
					next = read();
				}
				buffer.append(candidateEnding);

			}

			if (buffer != null && buffer.length() > 0) {
				return buffer.toString();
			}
			return null;

		}

		/**
		 * Check for end of line and accumulate a buffer for next time.
		 * 
		 * @param buffer the current line excluding the candidate ending
		 * @param candidate a buffer containing accumulated state
		 * @param next the next character (or -1 for end of file)
		 * @return true if the values together signify the end of a file
		 */
		private boolean isEndOfLine(StringBuilder buffer, StringBuilder candidate, int next) {

			if (next == -1) {
				return true;
			}

			char c = (char) next;
			if (ending.charAt(0) == c || candidate.length() > 0) {
				candidate.append(c);
			}

			if (candidate.length() == 0) {
				buffer.append(c);
				return false;
			}

			boolean end = ending.equals(candidate.toString());
			if (end) {
				candidate.delete(0, candidate.length());
			}
			else if (candidate.length() >= ending.length()) {
				buffer.append(candidate);
				candidate.delete(0, candidate.length());
			}

			return end;

		}
	}

}
