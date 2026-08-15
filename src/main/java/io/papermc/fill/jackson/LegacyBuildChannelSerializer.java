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
package io.papermc.fill.jackson;

import io.papermc.fill.model.BuildChannel;
import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

@Deprecated(forRemoval = true)
@NullMarked
public class LegacyBuildChannelSerializer extends StdSerializer<BuildChannel> {
  public LegacyBuildChannelSerializer() {
    super(BuildChannel.class);
  }

  @Override
  public void serialize(final BuildChannel value, final JsonGenerator gen, final SerializationContext context) throws JacksonException {
    gen.writeString(switch (value) {
      case ALPHA -> "experimental";
      case BETA -> "experimental";
      case STABLE -> "default";
      case RECOMMENDED -> "default";
    });
  }
}
