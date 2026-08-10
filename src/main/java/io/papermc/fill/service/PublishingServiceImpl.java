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

import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.database.VersionRepository;
import io.papermc.fill.exception.SessionConflictException;
import io.papermc.fill.model.Numbered;
import java.util.OptionalInt;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class PublishingServiceImpl implements PublishingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PublishingServiceImpl.class);

  private final VersionRepository versions;
  private final BuildRepository builds;

  @Autowired
  public PublishingServiceImpl(
    final VersionRepository versions,
    final BuildRepository builds
  ) {
    this.versions = versions;
    this.builds = builds;
  }

  @Override
  public int allocateBuildNumber(
    final String session,
    final VersionEntity version,
    @Deprecated(forRemoval = true)
    final OptionalInt requested
  ) {
    if (version.isPastPublishingSession(session)) {
      throw new SessionConflictException(String.format("Session %s expired.", session));
    }

    final int allocatedBuildNumber;
    if (requested.isPresent()) {
      LOGGER.warn("Manually requested build number {} for version {}", requested, version._id());
      allocatedBuildNumber = requested.getAsInt();
    } else if (version.nextBuildNumber() == 0) {
      final OptionalInt maxBuildNumber = this.builds
        .findAllByVersion(version)
        .mapToInt(Numbered::number)
        .max();
      allocatedBuildNumber = maxBuildNumber.orElse(0) + 1;
    } else {
      allocatedBuildNumber = version.nextBuildNumber();
    }
    version.setPublishingSession(session);
    version.setNextBuildNumber(allocatedBuildNumber + 1);
    this.versions.save(version);
    return allocatedBuildNumber;
  }
}
