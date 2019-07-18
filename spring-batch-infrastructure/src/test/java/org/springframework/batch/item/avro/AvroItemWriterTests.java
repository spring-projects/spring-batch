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

package org.springframework.batch.item.avro;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.avro.example.User;
import org.springframework.batch.item.avro.support.AvroItemWriterTestSupport;

/**
 * @author David Turanski
 */
public class AvroItemWriterTests extends AvroItemWriterTestSupport {

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void writeAvroGeneratedToFileWithDataFileWriter() throws Exception {

        DatumWriter<User> datumWriter = new SpecificDatumWriter<>(User.class);
        DataFileWriter<User> dataFileWriter = new DataFileWriter<>(datumWriter);
        File outputFile = folder.newFile();
        AvroItemWriter avroItemWriter = new AvroItemWriter<>(outputFile, dataFileWriter,
                avroGeneratedUsers().get(0).getSchema());

        avroItemWriter.open(new ExecutionContext());
        avroItemWriter.write(this.avroGeneratedUsers());
        avroItemWriter.close();

        verifyRecordsWithEmbeddedHeader(Files.readAllBytes(outputFile.toPath()),avroGeneratedUsers(), User.class);
    }

    @Test
    public void writeAvroGeneratedToFileWithSchemaFile() throws Exception {

        File outputFile = folder.newFile();
        AvroItemWriter avroItemWriter = new AvroItemWriter<>(outputFile, User.class, schemaResource.getFile());

        avroItemWriter.open(new ExecutionContext());
        avroItemWriter.write(this.avroGeneratedUsers());
        avroItemWriter.close();

        verifyRecordsWithEmbeddedHeader(Files.readAllBytes(outputFile.toPath()),avroGeneratedUsers(), User.class);
    }

    @Test
    public void writeAvroGeneratedWithDatumWriter() throws Exception {

        DatumWriter<User> datumWriter = new SpecificDatumWriter<>(User.class);

        AvroItemWriter avroItemWriter = new AvroItemWriter<>(outputStream, datumWriter);

        avroItemWriter.open(new ExecutionContext());
        avroItemWriter.write(this.avroGeneratedUsers());
        avroItemWriter.close();

        verifyRecords(outputStream.toByteArray(), this.avroGeneratedUsers(), User.class, false);
    }


    @Test
    public void writePojosWithDatumWriter() throws Exception {

        DatumWriter<PlainOldUser> datumWriter = new GenericDatumWriter<>(PlainOldUser.SCHEMA);

        AvroItemWriter avroItemWriter = new AvroItemWriter<>(outputStream, datumWriter);
        avroItemWriter.open(new ExecutionContext());
        avroItemWriter.write(this.genericPlainOldUsers());
        avroItemWriter.close();

        verifyPojos(outputStream.toByteArray(), genericPlainOldUsers(),PlainOldUser.SCHEMA);
    }

    @Test
    public void writeGenericWithDatumWriter() throws Exception {

        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(PlainOldUser.SCHEMA);

        AvroItemWriter avroItemWriter = new AvroItemWriter<>(outputStream, datumWriter);
        avroItemWriter.open(new ExecutionContext());
        avroItemWriter.write(this.genericPlainOldUsers());
        avroItemWriter.close();

        verifyPojos(outputStream.toByteArray(), genericPlainOldUsers(),PlainOldUser.SCHEMA);
    }

}
