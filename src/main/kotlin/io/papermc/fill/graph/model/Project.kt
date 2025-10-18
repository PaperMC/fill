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
import io.papermc.fill.database.FamilyRepository
import io.papermc.fill.database.ProjectEntity
import io.papermc.fill.database.VersionRepository
import io.papermc.fill.service.StorageService
import io.papermc.fill.model.Family as JavaFamily
import io.papermc.fill.model.Version as JavaVersion

data class Project(
  val id: String,
  val name: String,
  @GraphQLIgnore
  val entity: ProjectEntity,
  @GraphQLIgnore
  val familyRepository: FamilyRepository,
  @GraphQLIgnore
  val versionRepository: VersionRepository,
  @GraphQLIgnore
  val buildRepository: BuildRepository,
  @GraphQLIgnore
  val storageService: StorageService
) {
  fun families(): List<Family> {
    return familyRepository.findAllByProject(entity)
      .sorted(JavaFamily.COMPARATOR_CREATED_AT_REVERSE)
      .map { it.toGraphQL() }
      .toList()
  }

  fun family(id: String): Family? {
    return familyRepository.findByProjectAndName(entity, id)
      .map { it.toGraphQL() }
      .orElse(null)
  }

  fun versions(filterBy: VersionFilters? = null, last: Int? = null): List<Version> {
    var versionStream = versionRepository.findAllByProject(entity)

    if (filterBy != null) {
      val familyId = filterBy.familyId
      if (familyId != null) {
        versionStream = versionStream.filter { it.family().id() == familyId }
      }
      val supportStatus = filterBy.supportStatus
      if (supportStatus != null) {
        versionStream = versionStream.filter(JavaVersion.isSupportStatus(supportStatus))
      }
    }

    if (last != null) {
      versionStream = versionStream.limit(last.toLong())
    }

    return versionStream.map { it.toGraphQL(buildRepository, storageService) }.toList()
  }

  fun version(id: String): Version? {
    return versionRepository.findByProjectAndName(entity, id)
      .map { it.toGraphQL(buildRepository, storageService) }
      .orElse(null)
  }
}

fun ProjectEntity.toGraphQL(
  familyRepository: FamilyRepository,
  versionRepository: VersionRepository,
  buildRepository: BuildRepository,
  storageService: StorageService
): Project = Project(
  id = this.id(),
  name = this.name(),
  entity = this,
  familyRepository = familyRepository,
  versionRepository = versionRepository,
  buildRepository = buildRepository,
  storageService = storageService
)
