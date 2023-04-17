// Copyright 2023 Tabular Technologies Inc.
package io.tabular.connect.poc.commit;

import static io.tabular.connect.poc.commit.Message.Type.BEGIN_COMMIT;
import static io.tabular.connect.poc.commit.Message.Type.DATA_FILES;

import io.tabular.connect.poc.IcebergWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.util.Pair;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTaskContext;

public class Worker extends Channel {

  private final IcebergWriter writer;
  private final SinkTaskContext context;

  public Worker(
      Catalog catalog,
      TableIdentifier tableIdentifier,
      Map<String, String> props,
      SinkTaskContext context) {
    super(props);
    this.writer = new IcebergWriter(catalog, tableIdentifier);
    this.context = context;
  }

  @Override
  protected void receive(Message message) {
    if (message.getType() == BEGIN_COMMIT) {
      Pair<List<DataFile>, Map<TopicPartition, Long>> commitResult = writer.commit();
      Message filesMessage =
          Message.builder()
              .type(DATA_FILES)
              .dataFiles(commitResult.first())
              .offsets(commitResult.second())
              .assignments(context.assignment())
              .build();
      send(filesMessage);
    }
  }

  @Override
  public void stop() {
    super.stop();
    writer.close();
  }

  public void save(Collection<SinkRecord> sinkRecords) {
    writer.write(sinkRecords);
  }
}