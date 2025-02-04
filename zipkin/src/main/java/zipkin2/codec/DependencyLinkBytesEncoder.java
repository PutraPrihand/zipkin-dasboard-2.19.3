/*
 * Copyright 2015-2020 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.codec;

import java.util.List;
import zipkin2.DependencyLink;
import zipkin2.internal.JsonCodec;
import zipkin2.internal.WriteBuffer;
import zipkin2.internal.WriteBuffer.Writer;

import static zipkin2.internal.JsonEscaper.jsonEscape;
import static zipkin2.internal.JsonEscaper.jsonEscapedSizeInBytes;
import static zipkin2.internal.WriteBuffer.asciiSizeInBytes;

public enum DependencyLinkBytesEncoder implements BytesEncoder<DependencyLink> {
  JSON_V1 {
    @Override public Encoding encoding() {
      return Encoding.JSON;
    }

    @Override public int sizeInBytes(DependencyLink input) {
      return WRITER.sizeInBytes(input);
    }

    @Override public byte[] encode(DependencyLink link) {
      return JsonCodec.write(WRITER, link);
    }

    @Override public byte[] encodeList(List<DependencyLink> links) {
      return JsonCodec.writeList(WRITER, links);
    }
  };

  static final Writer<DependencyLink> WRITER = new Writer<DependencyLink>() {
    @Override public int sizeInBytes(DependencyLink value) {
      int sizeInBytes = 37; // {"parent":"","child":"","callCount":}
      sizeInBytes += jsonEscapedSizeInBytes(value.parent());
      sizeInBytes += jsonEscapedSizeInBytes(value.child());
      sizeInBytes += asciiSizeInBytes(value.callCount());
      if (value.errorCount() > 0) {
        sizeInBytes += 14; // ,"errorCount":
        sizeInBytes += asciiSizeInBytes(value.errorCount());
      }
      return sizeInBytes;
    }

    @Override public void write(DependencyLink value, WriteBuffer b) {
      b.writeAscii("{\"parent\":\"");
      b.writeUtf8(jsonEscape(value.parent()));
      b.writeAscii("\",\"child\":\"");
      b.writeUtf8(jsonEscape(value.child()));
      b.writeAscii("\",\"callCount\":");
      b.writeAscii(value.callCount());
      if (value.errorCount() > 0) {
        b.writeAscii(",\"errorCount\":");
        b.writeAscii(value.errorCount());
      }
      b.writeByte('}');
    }

    @Override public String toString() {
      return "DependencyLink";
    }
  };
}
