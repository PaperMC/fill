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

import com.google.common.annotations.VisibleForTesting;
import io.papermc.fill.database.BuildRepository;
import io.papermc.fill.database.VersionEntity;
import io.papermc.fill.model.Numbered;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@NullMarked
@Service
public class PublishingServiceImpl implements PublishingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(PublishingServiceImpl.class);

  private final BuildRepository builds;

  @Autowired
  public PublishingServiceImpl(
    final BuildRepository builds
  ) {
    this.builds = builds;
  }

  @Override
  public int getNextBuildNumber(
    final VersionEntity version,
    @Deprecated(forRemoval = true)
    final OptionalInt requested
  ) {
    if (requested.isPresent()) {
      LOGGER.warn("Manually requested build number {} for version {}", requested, version._id());
    }
    return getNextBuildNumber(() -> this.builds.findAllByVersion(version).toList(), requested);
  }

  @VisibleForTesting
  static int getNextBuildNumber(
    final Supplier<List<? extends Numbered>> builds,
    @Deprecated(forRemoval = true)
    final OptionalInt requested
  ) {
    if (requested.isPresent()) {
      return requested.getAsInt();
    }
    final OptionalInt maxBuildNumber = builds
      .get()
      .stream()
      .mapToInt(Numbered::number)
      .max();
    if (maxBuildNumber.isPresent()) {
      return maxBuildNumber.getAsInt() + 1;
    }
    return 1;
  }
}
