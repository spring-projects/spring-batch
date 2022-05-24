/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * The {@link BufferedReaderFactory} strategy provides custom implementation of {@link BufferedReader}, that splits a
 * stream based on a fixed line length (rather than the usual convention based on plain text).
 *
 * @author Parikshit Dutta
 *
 * @since 4.3
 */
public class FixedLengthBufferedReaderFactory implements BufferedReaderFactory {

	/**
	 * The default line length value.
	 */
	private static final int DEFAULT_LINE_LENGTH = 32;
	private int lineLength = DEFAULT_LINE_LENGTH;

	/**
	 * @param lineLength {@link int} indicating what defines the length of a "line".
	 */
	public void setLineLength(int lineLength) {
		this.lineLength = lineLength;
	}

	@Override
	public BufferedReader create(Resource resource, String encoding) throws UnsupportedEncodingException, IOException {
		return new FixedLengthBufferedReader(new InputStreamReader(resource.getInputStream(), encoding), lineLength);
	}

	/**
	 * BufferedReader extension that reads lines based on a given length, rather
	 * than the usual plain text conventions.
	 *
	 * @author Parikshit Dutta
	 *
	 */
	private final class FixedLengthBufferedReader extends BufferedReader {

		private int length;

		/**
		 * @param in A reader
		 * @param length A integer to determine the read length
		 */
		private FixedLengthBufferedReader(Reader in, int length) {
			super(in);
			this.length = length;
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
				buffer.append((char)next);

				for (int i = 1; i < length; i++) {
					next = read();
					if (next != -1) {
						buffer.append((char)next);
					} else {
						break;
					}
				}
			}

			if (buffer != null && buffer.length() > 0) {
				return buffer.toString();
			}
			return null;
		}
	}
}
