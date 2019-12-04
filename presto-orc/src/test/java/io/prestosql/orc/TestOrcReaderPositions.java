/*
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
package io.prestosql.orc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.airlift.slice.Slice;
import io.prestosql.orc.metadata.CompressionKind;
import io.prestosql.orc.metadata.Footer;
import io.prestosql.orc.metadata.OrcColumnId;
import io.prestosql.orc.metadata.statistics.IntegerStatistics;
import io.prestosql.spi.Page;
import io.prestosql.spi.block.Block;
import io.prestosql.spi.connector.ConnectorOperationContext;
import io.prestosql.spi.eventlistener.TracerEvent;
import io.prestosql.spi.tracer.ConnectorEventEmitter;
import io.prestosql.spi.tracer.ConnectorTracer;
import io.prestosql.spi.tracer.DefaultTracer;
import io.prestosql.spi.tracer.PayloadBuilder;
import io.prestosql.spi.tracer.Tracer;
import io.prestosql.spi.tracer.TracerEventTypeSupplier;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.FileSinkOperator;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat;
import org.apache.hadoop.hive.ql.io.orc.OrcSerde;
import org.apache.hadoop.hive.ql.io.orc.Writer;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.Serializer;
import org.apache.hadoop.hive.serde2.objectinspector.SettableStructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.io.Writable;
import org.apache.orc.NullMemoryManager;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.prestosql.orc.OrcReader.BATCH_SIZE_GROWTH_FACTOR;
import static io.prestosql.orc.OrcReader.INITIAL_BATCH_SIZE;
import static io.prestosql.orc.OrcReader.MAX_BATCH_SIZE;
import static io.prestosql.orc.OrcTester.Format.ORC_12;
import static io.prestosql.orc.OrcTester.READER_OPTIONS;
import static io.prestosql.orc.OrcTester.createCustomOrcRecordReader;
import static io.prestosql.orc.OrcTester.createOrcRecordWriter;
import static io.prestosql.orc.OrcTester.createSettableStructObjectInspector;
import static io.prestosql.orc.OrcTracerEventType.READ_BLOCK_END;
import static io.prestosql.orc.OrcTracerEventType.READ_BLOCK_START;
import static io.prestosql.orc.OrcTracerEventType.READ_ORC_TAIL_END;
import static io.prestosql.orc.OrcTracerEventType.READ_ORC_TAIL_START;
import static io.prestosql.orc.OrcTracerEventType.READ_STRIPE_END;
import static io.prestosql.orc.OrcTracerEventType.READ_STRIPE_START;
import static io.prestosql.spi.connector.ConnectorOperationContext.createNoOpConnectorOperationContext;
import static io.prestosql.spi.type.BigintType.BIGINT;
import static io.prestosql.spi.type.VarcharType.VARCHAR;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.hadoop.hive.ql.io.orc.CompressionKind.SNAPPY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestOrcReaderPositions
{
    @Test
    public void testEntireFile()
            throws Exception
    {
        try (TempFile tempFile = new TempFile()) {
            createMultiStripeFile(tempFile.getFile());
            List<TracerEvent> tracerEvents = Collections.synchronizedList(new ArrayList<>());
            Tracer engineTracer = DefaultTracer.createBasicTracer(event -> tracerEvents.add(event), "node", URI.create("http://test.com"), "0", true);
            ConnectorOperationContext connectorOperationContext = (new ConnectorOperationContext.Builder()).withConnectorTracer(Optional.of(new TestingOrcTracer(new ConnectorEventEmitter(engineTracer)))).build();

            try (OrcRecordReader reader = createCustomOrcRecordReader(tempFile, OrcPredicate.TRUE, BIGINT, MAX_BATCH_SIZE, connectorOperationContext)) {
                assertEquals(reader.getReaderRowCount(), 100);
                assertEquals(reader.getReaderPosition(), 0);
                assertEquals(reader.getFileRowCount(), reader.getReaderRowCount());
                assertEquals(reader.getFilePosition(), reader.getReaderPosition());

                for (int i = 0; i < 5; i++) {
                    Page page = reader.nextPage().getLoadedPage();
                    assertEquals(page.getPositionCount(), 20);
                    assertEquals(reader.getReaderPosition(), i * 20L);
                    assertEquals(reader.getFilePosition(), reader.getReaderPosition());
                    assertCurrentBatch(page, i);
                }

                assertNull(reader.nextPage());
                assertEquals(reader.getReaderPosition(), 100);
                assertEquals(reader.getFilePosition(), reader.getReaderPosition());
                TracerEventTypeSupplier[] expectedActions = {READ_ORC_TAIL_START, READ_ORC_TAIL_END, READ_STRIPE_START, READ_STRIPE_END, READ_BLOCK_START, READ_BLOCK_END};
                checkContainAllTracerActions(expectedActions, tracerEvents);
            }
        }
    }

    @Test
    public void testStripeSkipping()
            throws Exception
    {
        try (TempFile tempFile = new TempFile()) {
            createMultiStripeFile(tempFile.getFile());

            // test reading second and fourth stripes
            OrcPredicate predicate = (numberOfRows, allColumnStatistics) -> {
                if (numberOfRows == 100) {
                    return true;
                }
                IntegerStatistics stats = allColumnStatistics.get(new OrcColumnId(1)).getIntegerStatistics();
                return ((stats.getMin() == 60) && (stats.getMax() == 117)) ||
                        ((stats.getMin() == 180) && (stats.getMax() == 237));
            };

            try (OrcRecordReader reader = createCustomOrcRecordReader(tempFile, predicate, BIGINT, MAX_BATCH_SIZE, createNoOpConnectorOperationContext())) {
                assertEquals(reader.getFileRowCount(), 100);
                assertEquals(reader.getReaderRowCount(), 40);
                assertEquals(reader.getFilePosition(), 0);
                assertEquals(reader.getReaderPosition(), 0);

                // second stripe
                Page page = reader.nextPage().getLoadedPage();
                assertEquals(page.getPositionCount(), 20);
                assertEquals(reader.getReaderPosition(), 0);
                assertEquals(reader.getFilePosition(), 20);
                assertCurrentBatch(page, 1);

                // fourth stripe
                page = reader.nextPage().getLoadedPage();
                assertEquals(page.getPositionCount(), 20);
                assertEquals(reader.getReaderPosition(), 20);
                assertEquals(reader.getFilePosition(), 60);
                assertCurrentBatch(page, 3);

                page = reader.nextPage();
                assertNull(page);
                assertEquals(reader.getReaderPosition(), 40);
                assertEquals(reader.getFilePosition(), 100);
            }
        }
    }

    @Test
    public void testRowGroupSkipping()
            throws Exception
    {
        try (TempFile tempFile = new TempFile()) {
            // create single strip file with multiple row groups
            int rowCount = 142_000;
            createSequentialFile(tempFile.getFile(), rowCount);

            // test reading two row groups from middle of file
            OrcPredicate predicate = (numberOfRows, allColumnStatistics) -> {
                if (numberOfRows == rowCount) {
                    return true;
                }
                IntegerStatistics stats = allColumnStatistics.get(new OrcColumnId(1)).getIntegerStatistics();
                return (stats.getMin() == 50_000) || (stats.getMin() == 60_000);
            };

            try (OrcRecordReader reader = createCustomOrcRecordReader(tempFile, predicate, BIGINT, MAX_BATCH_SIZE, createNoOpConnectorOperationContext())) {
                assertEquals(reader.getFileRowCount(), rowCount);
                assertEquals(reader.getReaderRowCount(), rowCount);
                assertEquals(reader.getFilePosition(), 0);
                assertEquals(reader.getReaderPosition(), 0);

                long position = 50_000;
                while (true) {
                    Page page = reader.nextPage();
                    if (page == null) {
                        break;
                    }
                    page = page.getLoadedPage();

                    Block block = page.getBlock(0);
                    for (int i = 0; i < block.getPositionCount(); i++) {
                        assertEquals(BIGINT.getLong(block, i), position + i);
                    }

                    assertEquals(reader.getFilePosition(), position);
                    assertEquals(reader.getReaderPosition(), position);
                    position += page.getPositionCount();
                }

                assertEquals(position, 70_000);
                assertEquals(reader.getFilePosition(), rowCount);
                assertEquals(reader.getReaderPosition(), rowCount);
            }
        }
    }

    @Test
    public void testBatchSizesForVariableWidth()
            throws Exception
    {
        // the test creates a table with one column and 10 row groups (i.e., 100K rows)
        // the 1st row group has strings with each of length 300,
        // the 2nd row group has strings with each of length 600,
        // the 3rd row group has strings with each of length 900, and so on
        // the test is to show when loading those strings,
        // we are first bounded by MAX_BATCH_SIZE = 1024 rows because 1024 X 900B < 1MB
        // then bounded by MAX_BLOCK_SIZE = 1MB because 1024 X 1200B > 1MB
        try (TempFile tempFile = new TempFile()) {
            // create single strip file with multiple row groups
            int rowsInRowGroup = 10000;
            int rowGroupCounts = 10;
            int baseStringBytes = 300;
            int rowCount = rowsInRowGroup * rowGroupCounts;
            createGrowingSequentialFile(tempFile.getFile(), rowCount, rowsInRowGroup, baseStringBytes);

            try (OrcRecordReader reader = createCustomOrcRecordReader(tempFile, OrcPredicate.TRUE, VARCHAR, MAX_BATCH_SIZE, createNoOpConnectorOperationContext())) {
                assertEquals(reader.getFileRowCount(), rowCount);
                assertEquals(reader.getReaderRowCount(), rowCount);
                assertEquals(reader.getFilePosition(), 0);
                assertEquals(reader.getReaderPosition(), 0);

                // each value's length = original value length + 4 bytes to denote offset + 1 byte to denote if null
                int currentStringBytes = baseStringBytes + Integer.BYTES + Byte.BYTES;
                int rowCountsInCurrentRowGroup = 0;
                while (true) {
                    Page page = reader.nextPage();
                    if (page == null) {
                        break;
                    }
                    page = page.getLoadedPage();

                    rowCountsInCurrentRowGroup += page.getPositionCount();

                    Block block = page.getBlock(0);
                    if (MAX_BATCH_SIZE * currentStringBytes <= READER_OPTIONS.getMaxBlockSize().toBytes()) {
                        // Either we are bounded by 1024 rows per batch, or it is the last batch in the row group
                        // For the first 3 row groups, the strings are of length 300, 600, and 900 respectively
                        // So the loaded data is bounded by MAX_BATCH_SIZE
                        assertTrue(block.getPositionCount() == MAX_BATCH_SIZE || rowCountsInCurrentRowGroup == rowsInRowGroup);
                    }
                    else {
                        // Either we are bounded by 1MB per batch, or it is the last batch in the row group
                        // From the 4th row group, the strings are have length > 1200
                        // So the loaded data is bounded by MAX_BLOCK_SIZE
                        assertTrue(block.getPositionCount() == READER_OPTIONS.getMaxBlockSize().toBytes() / currentStringBytes || rowCountsInCurrentRowGroup == rowsInRowGroup);
                    }

                    if (rowCountsInCurrentRowGroup == rowsInRowGroup) {
                        rowCountsInCurrentRowGroup = 0;
                        currentStringBytes += baseStringBytes;
                    }
                    else if (rowCountsInCurrentRowGroup > rowsInRowGroup) {
                        fail("read more rows in the current row group");
                    }
                }
            }
        }
    }

    @Test
    public void testBatchSizesForFixedWidth()
            throws Exception
    {
        // the test creates a table with one column and 10 row groups
        // the each row group has bigints of length 8 in bytes,
        // the test is to show that the loaded data is always bounded by MAX_BATCH_SIZE because 1024 X 8B < 1MB
        try (TempFile tempFile = new TempFile()) {
            // create single strip file with multiple row groups
            int rowsInRowGroup = 10_000;
            int rowGroupCounts = 10;
            int rowCount = rowsInRowGroup * rowGroupCounts;
            createSequentialFile(tempFile.getFile(), rowCount);

            try (OrcRecordReader reader = createCustomOrcRecordReader(tempFile, OrcPredicate.TRUE, BIGINT, MAX_BATCH_SIZE, createNoOpConnectorOperationContext())) {
                assertEquals(reader.getFileRowCount(), rowCount);
                assertEquals(reader.getReaderRowCount(), rowCount);
                assertEquals(reader.getFilePosition(), 0);
                assertEquals(reader.getReaderPosition(), 0);

                int rowCountsInCurrentRowGroup = 0;
                while (true) {
                    Page page = reader.nextPage();
                    if (page == null) {
                        break;
                    }
                    page = page.getLoadedPage();
                    rowCountsInCurrentRowGroup += page.getPositionCount();

                    Block block = page.getBlock(0);
                    // 8 bytes per row; 1024 row at most given 1024 X 8B < 1MB
                    assertTrue(block.getPositionCount() == MAX_BATCH_SIZE || rowCountsInCurrentRowGroup == rowsInRowGroup);

                    if (rowCountsInCurrentRowGroup == rowsInRowGroup) {
                        rowCountsInCurrentRowGroup = 0;
                    }
                    else if (rowCountsInCurrentRowGroup > rowsInRowGroup) {
                        fail("read more rows in the current row group");
                    }
                }
            }
        }
    }

    @Test
    public void testReadUserMetadata()
            throws Exception
    {
        try (TempFile tempFile = new TempFile()) {
            Map<String, String> metadata = ImmutableMap.of(
                    "a", "ala",
                    "b", "ma",
                    "c", "kota");
            createFileWithOnlyUserMetadata(tempFile.getFile(), metadata);

            OrcDataSource orcDataSource = new FileOrcDataSource(tempFile.getFile(), READER_OPTIONS);
            OrcReader orcReader = new OrcReader(orcDataSource, READER_OPTIONS, createNoOpConnectorOperationContext());
            Footer footer = orcReader.getFooter();
            Map<String, String> readMetadata = Maps.transformValues(footer.getUserMetadata(), Slice::toStringAscii);
            assertEquals(readMetadata, metadata);
        }
    }

    @Test
    public void testBatchSizeGrowth()
            throws Exception
    {
        try (TempFile tempFile = new TempFile()) {
            // Create a file with 5 stripes of 20 rows each.
            createMultiStripeFile(tempFile.getFile());

            try (OrcRecordReader reader = createCustomOrcRecordReader(tempFile, OrcPredicate.TRUE, BIGINT, INITIAL_BATCH_SIZE, createNoOpConnectorOperationContext())) {
                assertEquals(reader.getReaderRowCount(), 100);
                assertEquals(reader.getReaderPosition(), 0);
                assertEquals(reader.getFileRowCount(), reader.getReaderRowCount());
                assertEquals(reader.getFilePosition(), reader.getReaderPosition());

                // The batch size should start from INITIAL_BATCH_SIZE and grow by BATCH_SIZE_GROWTH_FACTOR.
                // For INITIAL_BATCH_SIZE = 1 and BATCH_SIZE_GROWTH_FACTOR = 2, the batchSize sequence should be
                // 1, 2, 4, 8, 5, 20, 20, 20, 20
                int totalReadRows = 0;
                int nextBatchSize = INITIAL_BATCH_SIZE;
                int expectedBatchSize = INITIAL_BATCH_SIZE;
                int rowCountsInCurrentRowGroup = 0;
                while (true) {
                    Page page = reader.nextPage();
                    if (page == null) {
                        break;
                    }
                    page = page.getLoadedPage();

                    assertEquals(page.getPositionCount(), expectedBatchSize);
                    assertEquals(reader.getReaderPosition(), totalReadRows);
                    assertEquals(reader.getFilePosition(), reader.getReaderPosition());
                    assertCurrentBatch(page, (int) reader.getReaderPosition(), page.getPositionCount());

                    if (nextBatchSize > 20 - rowCountsInCurrentRowGroup) {
                        nextBatchSize *= BATCH_SIZE_GROWTH_FACTOR;
                    }
                    else {
                        nextBatchSize = page.getPositionCount() * BATCH_SIZE_GROWTH_FACTOR;
                    }
                    rowCountsInCurrentRowGroup += page.getPositionCount();
                    totalReadRows += page.getPositionCount();
                    if (rowCountsInCurrentRowGroup == 20) {
                        rowCountsInCurrentRowGroup = 0;
                    }
                    else if (rowCountsInCurrentRowGroup > 20) {
                        fail("read more rows in the current row group");
                    }

                    expectedBatchSize = min(min(nextBatchSize, MAX_BATCH_SIZE), 20 - rowCountsInCurrentRowGroup);
                }

                assertEquals(reader.getReaderPosition(), 100);
                assertEquals(reader.getFilePosition(), reader.getReaderPosition());
            }
        }
    }

    private static void checkContainAllTracerActions(TracerEventTypeSupplier[] tracerActions, List<TracerEvent> tracerEvents)
    {
        try {
            // wait a second for events to propagate
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Map<String, Boolean> actionOccurance = new HashMap<>();
        for (TracerEventTypeSupplier action : tracerActions) {
            actionOccurance.put(action.toTracerEventType(), false);
        }
        for (TracerEvent event : tracerEvents) {
            actionOccurance.put(event.getEventType(), true);
        }
        for (Map.Entry<String, Boolean> entry : actionOccurance.entrySet()) {
            assertTrue(entry.getValue(), "action: " + entry.getKey() + " is not present");
        }
    }

    private static void assertCurrentBatch(Page page, int rowIndex, int batchSize)
    {
        Block block = page.getBlock(0);
        for (int i = 0; i < batchSize; i++) {
            assertEquals(BIGINT.getLong(block, i), (rowIndex + i) * 3);
        }
    }

    private static void assertCurrentBatch(Page page, int stripe)
    {
        Block block = page.getBlock(0);
        for (int i = 0; i < 20; i++) {
            assertEquals(BIGINT.getLong(block, i), ((stripe * 20L) + i) * 3);
        }
    }

    // write 5 stripes of 20 values each: (0,3,6,..,57), (60,..,117), .., (..297)
    private static void createMultiStripeFile(File file)
            throws IOException, ReflectiveOperationException, SerDeException
    {
        FileSinkOperator.RecordWriter writer = createOrcRecordWriter(file, ORC_12, CompressionKind.NONE, BIGINT);

        Serializer serde = new OrcSerde();
        SettableStructObjectInspector objectInspector = createSettableStructObjectInspector("test", BIGINT);
        Object row = objectInspector.create();
        StructField field = objectInspector.getAllStructFieldRefs().get(0);

        for (int i = 0; i < 300; i += 3) {
            if ((i > 0) && (i % 60 == 0)) {
                flushWriter(writer);
            }

            objectInspector.setStructFieldData(row, field, (long) i);
            Writable record = serde.serialize(row, objectInspector);
            writer.write(record);
        }

        writer.close(false);
    }

    private static void createFileWithOnlyUserMetadata(File file, Map<String, String> metadata)
            throws IOException
    {
        Configuration conf = new Configuration();
        OrcFile.WriterOptions writerOptions = OrcFile.writerOptions(conf)
                .memory(new NullMemoryManager())
                .inspector(createSettableStructObjectInspector("test", BIGINT))
                .compress(SNAPPY);
        Writer writer = OrcFile.createWriter(new Path(file.toURI()), writerOptions);
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            writer.addUserMetadata(entry.getKey(), ByteBuffer.wrap(entry.getValue().getBytes(UTF_8)));
        }
        writer.close();
    }

    private static void flushWriter(FileSinkOperator.RecordWriter writer)
            throws IOException, ReflectiveOperationException
    {
        Field field = OrcOutputFormat.class.getClassLoader()
                .loadClass(OrcOutputFormat.class.getName() + "$OrcRecordWriter")
                .getDeclaredField("writer");
        field.setAccessible(true);
        ((Writer) field.get(writer)).writeIntermediateFooter();
    }

    private static void createSequentialFile(File file, int count)
            throws IOException, SerDeException
    {
        FileSinkOperator.RecordWriter writer = createOrcRecordWriter(file, ORC_12, CompressionKind.NONE, BIGINT);

        Serializer serde = new OrcSerde();
        SettableStructObjectInspector objectInspector = createSettableStructObjectInspector("test", BIGINT);
        Object row = objectInspector.create();
        StructField field = objectInspector.getAllStructFieldRefs().get(0);

        for (int i = 0; i < count; i++) {
            objectInspector.setStructFieldData(row, field, (long) i);
            Writable record = serde.serialize(row, objectInspector);
            writer.write(record);
        }

        writer.close(false);
    }

    private static void createGrowingSequentialFile(File file, int count, int step, int initialLength)
            throws IOException, SerDeException
    {
        FileSinkOperator.RecordWriter writer = createOrcRecordWriter(file, ORC_12, CompressionKind.NONE, VARCHAR);

        Serializer serde = new OrcSerde();
        SettableStructObjectInspector objectInspector = createSettableStructObjectInspector("test", VARCHAR);
        Object row = objectInspector.create();
        StructField field = objectInspector.getAllStructFieldRefs().get(0);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < initialLength; i++) {
            builder.append("0");
        }
        String seedString = builder.toString();

        // gradually grow the length of a cell
        int previousLength = initialLength;
        for (int i = 0; i < count; i++) {
            if ((i / step + 1) * initialLength > previousLength) {
                previousLength = (i / step + 1) * initialLength;
                builder.append(seedString);
            }
            objectInspector.setStructFieldData(row, field, builder.toString());
            Writable record = serde.serialize(row, objectInspector);
            writer.write(record);
        }

        writer.close(false);
    }

    private static class TestingOrcTracer
            implements ConnectorTracer
    {
        private final ConnectorEventEmitter emitter;

        TestingOrcTracer(ConnectorEventEmitter emitter)
        {
            this.emitter = requireNonNull(emitter, "emitter is null");
        }

        @Override
        public void emitEvent(TracerEventTypeSupplier eventType, PayloadBuilder payloadBuilder)
        {
            emitter.emitEvent(eventType, payloadBuilder);
        }

        @Override
        public boolean isEnabled()
        {
            return emitter.isEnabled();
        }
    }
}
