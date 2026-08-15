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

import org.bson.types.ObjectId;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

@JacksonComponent
@NullMarked
public class ObjectIdSerializer extends StdSerializer<ObjectId> {
  public ObjectIdSerializer() {
    super(ObjectId.class);
  }

  @Override
  public void serialize(final ObjectId value, final JsonGenerator gen, final SerializationContext context) throws JacksonException {
    gen.writeString(value.toHexString());
  }
}
