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

import io.papermc.fill.database.BuildEntity;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.FamilyEntity;
import io.papermc.fill.database.FamilyRepository;
import io.papermc.fill.database.ProjectEntity;
import io.papermc.fill.database.ProjectRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.BuildNotFoundException;
import io.papermc.fill.exception.FamilyNotFoundException;
import io.papermc.fill.exception.ProjectNotFoundException;
import io.papermc.fill.exception.VersionNotFoundException;
import io.papermc.fill.model.request.VersionCheckRequest;
import io.papermc.fill.model.response.VersionCheckResponse;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class VersionCheckServiceImpl implements VersionCheckService {
  private static final VersionCheckResponse UP_TO_DATE = new VersionCheckResponse(VersionCheckResponse.Status.UP_TO_DATE, null);
  private final ProjectRepository projects;
  private final FamilyRepository families;
  private final VersionRepository versions;
  private final BuildRepository builds;

  @Autowired
  public VersionCheckServiceImpl(
    final ProjectRepository projects,
    final FamilyRepository families,
    final VersionRepository versions,
    final BuildRepository builds
  ) {
    this.projects = projects;
    this.families = families;
    this.versions = versions;
    this.builds = builds;
  }

  @Override
  public VersionCheckResponse check(final VersionCheckRequest request) {
    final ProjectEntity project = this.projects.findByKey(request.project()).orElseThrow(ProjectNotFoundException::new);
    final VersionEntity version = this.versions.findByProjectAndKey(project, request.version()).orElseThrow(VersionNotFoundException::new);
    final FamilyEntity family = this.families.findById(version.family()).orElseThrow(FamilyNotFoundException::new);
    final BuildEntity build = this.builds.findByVersionAndNumber(version, request.build()).orElseThrow(BuildNotFoundException::new);

    final int distanceFamily = findDistance(this.families.findAllByProject(project).toList(), family.key(), FamilyEntity::key);
    final int distanceVersion = findDistance(this.versions.findAllByFamily(family).toList(), version.key(), VersionEntity::key);
    final int distanceBuild = findDistance(this.builds.findAllByVersion(version).toList(), build.number(), BuildEntity::number);

    if (distanceFamily > 0 || distanceVersion > 0 || distanceBuild > 0) {
      return new VersionCheckResponse(VersionCheckResponse.Status.OUT_OF_DATE, new VersionCheckResponse.BehindBy(distanceFamily, distanceVersion, distanceBuild));
    }

    return UP_TO_DATE;
  }

  private static <T, K> int findDistance(final List<T> items, final K target, final Function<T, K> getter) {
    for (int i = 0; i < items.size(); i++) {
      if (getter.apply(items.get(i)).equals(target)) {
        return i;
      }
    }
    return Integer.MIN_VALUE;
  }
}
