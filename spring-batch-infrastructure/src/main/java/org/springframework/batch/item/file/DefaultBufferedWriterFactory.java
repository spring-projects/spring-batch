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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.springframework.batch.support.transaction.TransactionAwareBufferedWriter;

/**
 * An abstract factory implementation of {@link BufferedWriterFactory} for providing
 * default behaviour of {@link BufferedWriter} and {@link TransactionAwareBufferedWriter}.
 *
 * @author Parikshit Dutta
 *
 * @since 4.3
 */
public class DefaultBufferedWriterFactory implements BufferedWriterFactory  {

	@Override
	public BufferedWriter createBufferedWriter(FileChannel fileChannel, String encoding, boolean forceSync) {

		return new BufferedWriter(Channels.newWriter(fileChannel, encoding)) {
			@Override
			public void flush() throws IOException {
				super.flush();
				if (forceSync) {
					fileChannel.force(false);
				}
			}
		};
	}

	@Override
	public TransactionAwareBufferedWriter createTransactionAwareBufferedWriter(FileChannel fileChannel, String encoding,
			boolean forceSync, Runnable closeCallback) {

		TransactionAwareBufferedWriter writer = new TransactionAwareBufferedWriter(fileChannel, closeCallback);

		writer.setEncoding(encoding);
		writer.setForceSync(forceSync);
		return writer;
	}
}
