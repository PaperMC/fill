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
package io.papermc.fill.graph

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import io.papermc.fill.database.BuildRepository
import io.papermc.fill.database.FamilyRepository
import io.papermc.fill.database.ProjectRepository
import io.papermc.fill.database.VersionRepository
import io.papermc.fill.graph.model.*
import io.papermc.fill.model.Project as JavaProject
import io.papermc.fill.service.StorageService
import org.springframework.stereotype.Component

@Component
class ProjectQuery(
  private val projectRepository: ProjectRepository,
  private val familyRepository: FamilyRepository,
  private val versionRepository: VersionRepository,
  private val buildRepository: BuildRepository,
  private val storageService: StorageService
) {

  @GraphQLDescription("Get all projects")
  fun projects(): List<Project> {
    return projectRepository.findAll()
      .stream()
      .sorted(JavaProject.COMPARATOR_ID)
      .map { it.toGraphQL(familyRepository, versionRepository, buildRepository, storageService) }
      .toList()
  }

  @GraphQLDescription("Get a single project by ID")
  fun project(id: String): Project? {
    return projectRepository.findByName(id)
      .map { it.toGraphQL(familyRepository, versionRepository, buildRepository, storageService) }
      .orElse(null)
  }
}
