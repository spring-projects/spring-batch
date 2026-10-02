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

package org.springframework.batch.infrastructure.item.support.builder;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemReader;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chirag Tailor
 */
class ItemReaderBuilderTests {

    @Test
    void createsABuilderWithAllOfTheSpecifiedBehaviors() throws Exception {
        ItemReader<TestItemC> itemReader =
                ItemReaderBuilder.from(new ListItemReader<>(List.of(new TestItemA(3))))
//                [3]
                .flatMap(testItemA -> IntStream.range(0, testItemA.number())
                        .mapToObj(value -> new TestItemB(testItemA.number() * 3 + value))
                        .toList())
//                [9,10,11]
                .map(testItemB -> new TestItemC(testItemB.number() * 5))
//                [45,50,55]
                .filter(testItemC -> testItemC.number() % 2 == 0)
//                [50]
                .build();

        assertThat(Objects.requireNonNull(itemReader.read())).isEqualTo(new TestItemC(50));
        assertThat(itemReader.read()).isNull();
    }

    private record TestItemA(int number) {
    }

    private record TestItemB(int number) {
    }

    private record TestItemC(int number) {
    }
}
