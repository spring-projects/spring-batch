/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.core.test.football.internal;

import org.springframework.batch.core.test.football.domain.Player;
import org.springframework.batch.core.test.football.domain.PlayerDao;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class PlayerItemWriter implements ItemWriter<Player> {

	private PlayerDao playerDao;

	@Override
	public void write(Chunk<? extends Player> players) throws Exception {
		for (Player player : players) {
			playerDao.savePlayer(player);
		}
	}

	public void setPlayerDao(PlayerDao playerDao) {
		this.playerDao = playerDao;
	}

}
