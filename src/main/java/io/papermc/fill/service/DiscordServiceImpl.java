/*
 * Copyright 2024 PaperMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.papermc.fill.service;

import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.RestClient;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@ConditionalOnProperty("app.discord.token")
@NullMarked
@Service
public class DiscordServiceImpl implements DiscordService {
  private final RestClient rest;

  @Autowired
  public DiscordServiceImpl(
    final RestClient rest
  ) {
    this.rest = rest;
  }

  @Override
  public Mono<MessageData> createMessage(final long channel, final MessageCreateSpec request) {
    return this.rest.getChannelService().createMessage(channel, request.asRequest());
  }
}
