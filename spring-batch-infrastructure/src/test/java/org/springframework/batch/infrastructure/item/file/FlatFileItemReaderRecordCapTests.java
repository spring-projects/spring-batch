/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.file;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.infrastructure.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.infrastructure.item.file.separator.JsonRecordSeparatorPolicy;
import org.springframework.core.io.ByteArrayResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the per-record line-count and byte-count caps introduced to
 * {@link FlatFileItemReader} to bound the worst-case CPU and memory cost of the
 * multi-line-record accumulator when the configured {@link DefaultRecordSeparatorPolicy}
 * or {@link JsonRecordSeparatorPolicy} encounters a malformed input (e.g. an unbalanced
 * quote or brace).
 *
 * @author Mahmoud Ben Hassine
 */
class FlatFileItemReaderRecordCapTests {

	private FlatFileItemReader<String> reader;

	private final ExecutionContext executionContext = new ExecutionContext();

	@BeforeEach
	void setUp() {
		this.reader = new FlatFileItemReader<>(new PassThroughLineMapper());
	}

	/**
	 * Input with one unbalanced quote at the very first byte plus many newline- separated
	 * lines. Without the cap, {@code DefaultRecordSeparatorPolicy} folds the entire file
	 * into a single record and the accumulator does quadratic work. With the cap a
	 * {@link FlatFileParseException} fires as soon as the line limit is exceeded.
	 */
	@Test
	void defaultPolicyLineCapFiresOnUnbalancedQuote() {
		String input = "\"\n" + "x\n".repeat(2000);
		this.reader.setResource(new ByteArrayResource(input.getBytes()));
		this.reader.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
		this.reader.setMaxLinesPerRecord(50);
		this.reader.open(this.executionContext);

		FlatFileParseException ex = assertThrows(FlatFileParseException.class, this.reader::read);
		assertTrue(ex.getMessage().contains("exceeded the configured limit of 50 lines"),
				() -> "actual: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("unterminated record separator"),
				() -> "diagnostic hint should appear in the message: " + ex.getMessage());
	}

	/**
	 * Input with an unbalanced quote and a small line cap, but the line content is long
	 * enough that the byte cap trips first.
	 */
	@Test
	void defaultPolicyByteCapFiresWhenLineLimitWouldNotYet() {
		// 50 lines of 200 chars each is well over a 4-KiB byte cap but well under
		// a 200-line cap.
		StringBuilder input = new StringBuilder("\"\n");
		String filler = "x".repeat(200);
		for (int i = 0; i < 50; i++) {
			input.append(filler).append('\n');
		}
		this.reader.setResource(new ByteArrayResource(input.toString().getBytes()));
		this.reader.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
		this.reader.setMaxLinesPerRecord(200);
		this.reader.setMaxBytesPerRecord(4096);
		this.reader.open(this.executionContext);

		FlatFileParseException ex = assertThrows(FlatFileParseException.class, this.reader::read);
		assertTrue(ex.getMessage().contains("exceeded the configured limit of 4096 bytes"),
				() -> "actual: " + ex.getMessage());
	}

	/**
	 * Same input shape against {@link JsonRecordSeparatorPolicy} via an unbalanced
	 * opening brace.
	 */
	@Test
	void jsonPolicyLineCapFiresOnUnbalancedBrace() {
		String input = "{\n" + "x\n".repeat(2000);
		this.reader.setResource(new ByteArrayResource(input.getBytes()));
		this.reader.setRecordSeparatorPolicy(new JsonRecordSeparatorPolicy());
		this.reader.setMaxLinesPerRecord(50);
		this.reader.open(this.executionContext);

		FlatFileParseException ex = assertThrows(FlatFileParseException.class, this.reader::read);
		assertTrue(ex.getMessage().contains("exceeded the configured limit of 50 lines"),
				() -> "actual: " + ex.getMessage());
	}

	/**
	 * A legitimate multi-line record (well within the cap) must still round-trip cleanly.
	 * Three-line record using {@code DefaultRecordSeparatorPolicy}'s quoted-field
	 * continuation semantics.
	 */
	@Test
	void legitimateMultiLineRecordWorksWithinCap() throws Exception {
		String content = "\"hello\nworld\nfrom batch\"\n";
		this.reader.setResource(new ByteArrayResource(content.getBytes()));
		this.reader.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
		this.reader.setMaxLinesPerRecord(10);
		this.reader.open(this.executionContext);

		assertEquals("\"hello\nworld\nfrom batch\"", this.reader.read());
	}

	/**
	 * Raising the cap allows previously-rejected oversize records through. Confirms the
	 * cap is per-instance configurable, not a global constant.
	 */
	@Test
	void raisingTheCapAllowsLargerRecords() throws Exception {
		// 300-line legitimate quoted record (well-formed but verbose).
		StringBuilder content = new StringBuilder("\"start\n");
		for (int i = 0; i < 298; i++) {
			content.append("line").append(i).append('\n');
		}
		content.append("end\"\n");

		this.reader.setResource(new ByteArrayResource(content.toString().getBytes()));
		this.reader.setRecordSeparatorPolicy(new DefaultRecordSeparatorPolicy());
		this.reader.setMaxLinesPerRecord(500);
		this.reader.open(this.executionContext);

		String record = this.reader.read();
		// 300 lines folded into one, 'start' through 'end'
		assertTrue(record.startsWith("\"start"), () -> "actual start: " + record.substring(0, 20));
		assertTrue(record.endsWith("end\""), () -> "actual end: " + record.substring(record.length() - 20));
	}

	@Test
	void setMaxLinesPerRecordRejectsZeroOrNegative() {
		assertThrows(IllegalArgumentException.class, () -> this.reader.setMaxLinesPerRecord(0));
		assertThrows(IllegalArgumentException.class, () -> this.reader.setMaxLinesPerRecord(-1));
	}

	@Test
	void setMaxBytesPerRecordRejectsZeroOrNegative() {
		assertThrows(IllegalArgumentException.class, () -> this.reader.setMaxBytesPerRecord(0));
		assertThrows(IllegalArgumentException.class, () -> this.reader.setMaxBytesPerRecord(-1));
	}

}
