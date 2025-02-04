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
package zipkin2.storage.cassandra.v1;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.auto.value.AutoValue;
import java.util.Set;
import zipkin2.Call;
import zipkin2.storage.cassandra.internal.call.ResultSetFutureCall;

final class SelectTraceIdTimestampFromServiceName extends ResultSetFutureCall<ResultSet> {
  @AutoValue
  abstract static class Input {
    abstract String service_name();

    abstract long start_ts();

    abstract long end_ts();

    abstract int limit_();
  }

  static class Factory {
    final Session session;
    final PreparedStatement preparedStatement;
    final TimestampCodec timestampCodec;

    Factory(Session session, TimestampCodec timestampCodec, Set<Integer> buckets) {
      this.session = session;
      this.timestampCodec = timestampCodec;
      this.preparedStatement =
          session.prepare(
              QueryBuilder.select("ts", "trace_id")
                  .from(Tables.SERVICE_NAME_INDEX)
                  .where(QueryBuilder.eq("service_name", QueryBuilder.bindMarker("service_name")))
                  .and(QueryBuilder.in("bucket", buckets))
                  .and(QueryBuilder.gte("ts", QueryBuilder.bindMarker("start_ts")))
                  .and(QueryBuilder.lte("ts", QueryBuilder.bindMarker("end_ts")))
                  .limit(QueryBuilder.bindMarker("limit_"))
                  .orderBy(QueryBuilder.desc("ts")));
    }

    Call<Set<Pair>> newCall(String serviceName, long endTs, long lookback, int limit) {
      long startTs = Math.max(endTs - lookback, 0); // >= 1970
      Input input =
          new AutoValue_SelectTraceIdTimestampFromServiceName_Input(
              serviceName, startTs, endTs, limit);

      return new SelectTraceIdTimestampFromServiceName(this, input)
          .flatMap(new AccumulateTraceIdTsLong(timestampCodec));
    }
  }

  final Factory factory;
  final Input input;

  SelectTraceIdTimestampFromServiceName(Factory factory, Input input) {
    this.factory = factory;
    this.input = input;
  }

  @Override
  protected ResultSetFuture newFuture() {
    Statement bound =
        factory
            .preparedStatement
            .bind()
            .setString("service_name", input.service_name())
            .setBytesUnsafe("start_ts", factory.timestampCodec.serialize(input.start_ts()))
            .setBytesUnsafe("end_ts", factory.timestampCodec.serialize(input.end_ts()))
            .setInt("limit_", input.limit_())
            .setFetchSize(Integer.MAX_VALUE); // NOTE in the new driver, we also set this to limit
    return factory.session.executeAsync(bound);
  }

  @Override public ResultSet map(ResultSet input) {
    return input;
  }

  @Override
  public String toString() {
    return input.toString().replace("Input", "SelectTraceIdTimestampFromServiceName");
  }

  @Override
  public SelectTraceIdTimestampFromServiceName clone() {
    return new SelectTraceIdTimestampFromServiceName(factory, input);
  }
}
