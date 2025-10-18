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

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import io.papermc.fill.database.BuildRepository
import io.papermc.fill.database.VersionEntity
import io.papermc.fill.model.SupportStatus
import io.papermc.fill.service.StorageService
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.time.LocalDate

data class Version(
  val id: String,
  val family: Family,
  val support: Support,
  val java: Java?,
  val createdAt: Instant,
  val updatedAt: Instant,
  @GraphQLIgnore
  val entity: VersionEntity,
  @GraphQLIgnore
  val buildRepository: BuildRepository,
  @GraphQLIgnore
  val storageService: StorageService
) {
  fun builds(filterBy: BuildFilters? = null, last: Int? = null): List<Build> {
    val pageable = if (last != null) Pageable.ofSize(last) else Pageable.unpaged()
    val channel = filterBy?.channel
    val buildStream = if (channel != null) {
      buildRepository.findByVersionAndOptionalChannel(entity, channel, pageable)
    } else {
      buildRepository.findAllByVersion(entity, pageable)
    }

    return buildStream.map { buildEntity ->
      val downloads = buildEntity.downloads().values.map { download ->
        val url = storageService.getDownloadUrl(
          buildEntity.project(),
          buildEntity.version(),
          buildEntity,
          download
        )
        download.toGraphQLWithUrl(url)
      }
      buildEntity.toGraphQL(downloads)
    }.toList()
  }
}

data class Support(
  val status: SupportStatus,
  val end: LocalDate?
)

fun VersionEntity.toGraphQL(
  buildRepository: BuildRepository,
  storageService: StorageService
): Version = Version(
  id = this.id(),
  family = this.family().toGraphQL(),
  support = Support(
    status = this.support().status(),
    end = this.support().end()
  ),
  java = this.java()?.toGraphQL(),
  createdAt = this.createdAt(),
  updatedAt = this.updatedAt(),
  entity = this,
  buildRepository = buildRepository,
  storageService = storageService
)
