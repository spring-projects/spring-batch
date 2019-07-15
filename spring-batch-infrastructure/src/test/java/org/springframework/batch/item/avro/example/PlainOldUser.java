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

package org.springframework.batch.item.avro.example;

import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.ReflectData;

public class PlainOldUser {
    public static final Schema schema = ReflectData.get().getSchema(PlainOldUser.class);
    private String name;

    private int favoriteNumber;

    private String favoriteColor;

    public PlainOldUser(String name, int favoriteNumber, String favoriteColor) {
        this.name = name;
        this.favoriteNumber = favoriteNumber;
        this.favoriteColor = favoriteColor;
    }

    public String getName() {
        return name;
    }

    public int getFavoriteNumber() {
        return favoriteNumber;
    }

    public String getFavoriteColor() {
        return favoriteColor;
    }

    public GenericRecord toGenericRecord() {
        GenericData.Record avroRecord = new GenericData.Record(schema);
        avroRecord.put("name", this.name);
        avroRecord.put("favoriteNumber", this.favoriteNumber);
        avroRecord.put("favoriteColor",this.favoriteColor);
        return avroRecord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlainOldUser that = (PlainOldUser) o;
        return favoriteNumber == that.favoriteNumber &&
                Objects.equals(name, that.name) &&
                Objects.equals(favoriteColor, that.favoriteColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, favoriteNumber, favoriteColor);
    }
}