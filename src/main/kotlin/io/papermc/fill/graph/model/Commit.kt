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
package io.papermc.fill.graph.model

import java.time.ZonedDateTime

data class Commit(
  val sha: String,
  val time: ZonedDateTime,
  val summary: String,
  val message: String
)

fun io.papermc.fill.model.Commit.toGraphQL(): Commit = Commit(
  sha = this.sha(),
  time = this.time().atZone(java.time.ZoneOffset.UTC),
  summary = this.summary(),
  message = this.message()
)

