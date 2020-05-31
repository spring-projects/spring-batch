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
import java.nio.channels.FileChannel;

import org.springframework.batch.support.transaction.TransactionAwareBufferedWriter;

/**
 * An abstract factory strategy for custom extensions of both {@link BufferedWriter} and
 * {@link TransactionAwareBufferedWriter} allowing customisation of the standard behaviour of the
 * <code>java.io</code> variety.
 *
 * @author Parikshit Dutta
 *
 * @since 4.3
 */
public interface BufferedWriterFactory {
	BufferedWriter createBufferedWriter(FileChannel fileChannel, String encoding, boolean forceSync);
	TransactionAwareBufferedWriter createTransactionAwareBufferedWriter(FileChannel fileChannel, String encoding,
			boolean forceSync, Runnable closeCallback);
}
