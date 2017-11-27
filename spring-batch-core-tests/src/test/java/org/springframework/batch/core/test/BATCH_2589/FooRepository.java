/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.batch.core.test.BATCH_2589;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;


/**
 * @author Jannik Hell
 */
public class FooRepository implements PagingAndSortingRepository<Foo, Integer> {

	private final List<String> inputFooList;

	FooRepository() {
		inputFooList = new ArrayList<>();
		// 0 - 9
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("B");
		inputFooList.add("B");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
		// 10 - 19
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("B");
		inputFooList.add("B");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
		// 20 - 29
		// flawed one: "F", Index: 24
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("F");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
		// 30 - 39
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("A");
		inputFooList.add("B");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
		inputFooList.add("C");
		inputFooList.add("B");
	}

	@Override
	public Iterable<Foo> findAll(Sort sort) {
		return null;
	}

	@Override
	public Page<Foo> findAll(Pageable pageable) {

		if (pageable.getOffset() >= inputFooList.size()) {
			return new PageImpl<>(new ArrayList<>());
		}

		long pageEndIndex = pageable.getOffset() + pageable.getPageSize();
		long endIndex = pageEndIndex <= inputFooList.size() ? pageEndIndex : inputFooList.size() - 1;

		List<String> inputValues = inputFooList.subList((int) pageable.getOffset(), (int) endIndex);

		List<Foo> fooValues = inputValues
				.stream()
				.map(value -> new Foo(FooEnum.valueOf(value)))
				.collect(Collectors.toList());

		return new PageImpl<>(fooValues);
	}

	@Override
	public <S extends Foo> S save(S entity) {
		return null;
	}

	@Override
	public <S extends Foo> Iterable<S> save(Iterable<S> entities) {
		return null;
	}

	@Override
	public Optional<Foo> findOne(Integer integer) {
		return null;
	}

	@Override
	public boolean exists(Integer integer) {
		return false;
	}

	@Override
	public Iterable<Foo> findAll() {
		return null;
	}

	@Override
	public Iterable<Foo> findAll(Iterable<Integer> integers) {
		return null;
	}

	@Override
	public long count() {
		return 0;
	}

	@Override
	public void delete(Integer integer) {

	}

	@Override
	public void delete(Foo entity) {

	}

	@Override
	public void delete(Iterable<? extends Foo> entities) {

	}

	@Override
	public void deleteAll() {

	}
}
