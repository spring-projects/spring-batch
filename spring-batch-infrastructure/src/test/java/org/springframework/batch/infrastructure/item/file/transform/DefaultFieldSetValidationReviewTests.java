/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Additional review coverage for GH-5571.
 *
 * @author Kanish S
 */
class DefaultFieldSetValidationReviewTests {

	@Test
	void symbolSnapshotIsIsolatedAndTracksReplacementFormat() {
		RecordingFieldSet fieldSet = new RecordingFieldSet("1,234.5");
		DecimalFormatSymbols snapshot = fieldSet.symbols();
		snapshot.setGroupingSeparator('_');
		snapshot.setDecimalSeparator('|');

		assertEquals(',', fieldSet.symbols().getGroupingSeparator());
		assertEquals('.', fieldSet.symbols().getDecimalSeparator());
		assertEquals(new BigDecimal("1234.5"), fieldSet.readBigDecimal(0));

		fieldSet.setNumberFormat(NumberFormat.getNumberInstance(Locale.GERMANY));

		assertEquals('.', fieldSet.symbols().getGroupingSeparator());
		assertEquals(',', fieldSet.symbols().getDecimalSeparator());
	}

	@Test
	void blankDecimalValidationDiffersForFloatingAndBigDecimalReads() {
		RecordingFieldSet fieldSet = new RecordingFieldSet("   ");

		assertEquals(BigDecimal.ONE, fieldSet.readBigDecimal(0, BigDecimal.ONE));
		assertTrue(fieldSet.decimalInputs.isEmpty());
		assertThrows(NumberFormatException.class, () -> fieldSet.readFloat(0));
		assertThrows(NumberFormatException.class, () -> fieldSet.readDouble(0));

		assertEquals(List.of("", ""), fieldSet.decimalInputs);
	}

	private static class RecordingFieldSet extends DefaultFieldSet {

		private final List<String> decimalInputs = new ArrayList<>();

		RecordingFieldSet(String token) {
			super(new String[] { token });
		}

		@Override
		protected void validateDecimal(String input) {
			decimalInputs.add(input);
		}

		DecimalFormatSymbols symbols() {
			return getDecimalFormatSymbols();
		}

	}

}
