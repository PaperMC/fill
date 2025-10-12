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
package io.papermc.fill.database;

import io.papermc.fill.model.BuildChannel;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@NullMarked
@Repository
public interface BuildRepository extends MongoRepository<BuildEntity, ObjectId> {
  default Stream<BuildEntity> findAllByVersion(final VersionEntity version) {
    return this.findAllByVersion(version, Pageable.unpaged());
  }

  default Stream<BuildEntity> findAllByVersion(
    final VersionEntity version,
    final Pageable pageable
  ) {
    return this.findAllByVersion(version._id(), pageable);
  }

  @Query(sort = "{'number': -1}")
  Stream<BuildEntity> findAllByVersion(
    final ObjectId version,
    final Pageable pageable
  );

  default Stream<BuildEntity> findAllByVersionAndChannelIn(
    final VersionEntity version,
    final @Nullable List<BuildChannel> channel,
    final Pageable pageable
  ) {
    return this.findAllByVersionAndChannelIn(version._id(), channel, pageable);
  }

  @Query(sort = "{'number': -1}")
  Stream<BuildEntity> findAllByVersionAndChannelIn(
    final ObjectId version,
    final @Nullable List<BuildChannel> channel,
    final Pageable pageable
  );

  default Stream<BuildEntity> findByVersionAndOptionalChannelIn(
    final VersionEntity version,
    final @Nullable List<BuildChannel> channel,
    final Pageable pageable
  ) {
    if (channel != null) {
      return this.findAllByVersionAndChannelIn(version, channel, pageable);
    } else {
      return this.findAllByVersion(version, pageable);
    }
  }

  Stream<BuildEntity> findAllByVersionIn(final Collection<ObjectId> version);

  default Optional<BuildEntity> findByVersionAndNumber(
    final VersionEntity version,
    final int number
  ) {
    return this.findByVersionAndNumber(version._id(), number);
  }

  Optional<BuildEntity> findByVersionAndNumber(
    final ObjectId version,
    final int number
  );
}
