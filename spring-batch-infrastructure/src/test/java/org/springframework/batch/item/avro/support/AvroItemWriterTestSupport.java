/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.batch.item.avro.support;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;
import org.springframework.batch.item.avro.AvroItemReader;
import org.springframework.batch.item.avro.builder.AvroItemReaderBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 */
public abstract class AvroItemWriterTestSupport extends AvroTestFixtures {

    /*
     * This item reader configured for Specific Avro types.
     */
    protected  <T> void verifyRecords(byte[] bytes, List<T> actual, Class<T> clazz, boolean embeddedHeader) throws Exception {
        doVerify(bytes, clazz, actual, embeddedHeader);
    }

    protected  <T> void verifyRecordsWithEmbeddedHeader(byte[] bytes, List<T> actual, Class<T> clazz) throws Exception {
        doVerify(bytes, clazz, actual, true);
    }


    private <T> void doVerify(byte[] bytes, Class<T> clazz, List<T> actual, boolean embeddedHeader) throws Exception {
        AvroItemReader<T> avroItemReader = new AvroItemReaderBuilder<T>()
                .type(clazz)
                .inputStream(new ByteArrayInputStream(bytes))
                .embeddedHeader(embeddedHeader)
                .build();

        avroItemReader.afterPropertiesSet();

        List<T> records = new ArrayList<>();
        T record;
        while ((record = avroItemReader.read()) != null) {
            records.add(record);
        }
        assertThat(records).hasSize(4);
        assertThat(records).containsExactlyInAnyOrder(actual.get(0), actual.get(1), actual.get(2), actual.get(3));
    }

    /*
     * This item reader configured for no embedded SCHEMA header.
     */
    protected  <T> void verifyPojos(byte[] bytes, List<T> actual, Schema schema) throws Exception {
        AvroItemReader<T> avroItemReader = new AvroItemReaderBuilder<T>()
                .inputStream(new ByteArrayInputStream(bytes))
                .schema(schema)
                .build();
        avroItemReader.afterPropertiesSet();


        List<T> records = new ArrayList<>();
        T record;
        while ((record = avroItemReader.read()) != null) {
            records.add(record);
        }
        assertThat(records).hasSize(4);
        assertThat(records).containsExactlyInAnyOrder(actual.get(0), actual.get(1), actual.get(2), actual.get(3));

    }

}
