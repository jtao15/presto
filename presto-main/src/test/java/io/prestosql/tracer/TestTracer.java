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
package io.prestosql.tracer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.airlift.json.JsonCodec;
import io.prestosql.spi.eventlistener.TracerEvent;
import io.prestosql.spi.tracer.DefaultTracer;
import io.prestosql.spi.tracer.Tracer;
import io.prestosql.spi.tracer.TracerEventType;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Map;

import static io.prestosql.spi.tracer.TracerEventType.PLAN_QUERY_END;
import static io.prestosql.spi.tracer.TracerEventType.PLAN_QUERY_START;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TestTracer
{
    private static final String QUERY_ID = "query";
    private static final String STAGE_ID = "stage";
    private static final String TASK_ID = "task";
    private static final String NODE_ID = "node";
    private static final URI TEST_URI = URI.create("http://test.com");
    private static final TracerEventType QUERY_EVENT = PLAN_QUERY_START;
    private static final TracerEventType TASK_EVENT = PLAN_QUERY_END;
    private static final Map<String, Object> PAYLOAD = ImmutableMap.of("key", "value");

    private final JsonCodec<Map<String, Object>> jsonCodec = JsonCodec.mapJsonCodec(String.class, Object.class);

    @Test
    public void testTracer()
    {
        ImmutableList.Builder<TracerEvent> tracerEvents = new ImmutableList.Builder();
        Tracer queryTracer = DefaultTracer.createBasicTracer(event -> tracerEvents.add(event), map -> jsonCodec.toJson(map), NODE_ID, TEST_URI, QUERY_ID, true);
        queryTracer.emitEvent(QUERY_EVENT, null);

        Tracer taskTracer = queryTracer.newTracerWithStageId(STAGE_ID).newTracerWithTaskId(TASK_ID);
        taskTracer.emitEvent(TASK_EVENT, () -> PAYLOAD);

        ImmutableList<TracerEvent> events = tracerEvents.build();
        assertEquals(events.size(), 2);
        TracerEvent queryEvent = events.get(0);
        assertEquals(queryEvent.getEventType(), QUERY_EVENT.name());
        assertEquals(queryEvent.getNodeId(), NODE_ID);
        assertEquals(queryEvent.getUri(), TEST_URI.getHost() + ":" + String.valueOf(TEST_URI.getPort()));
        assertEquals(queryEvent.getQueryId(), QUERY_ID);
        assertNull(queryEvent.getStageId());
        assertNull(queryEvent.getTaskId());
        assertNull(queryEvent.getPipelineId());
        assertNull(queryEvent.getSplitId());
        assertNull(queryEvent.getOperatorId());
        assertNull(queryEvent.getPayload());

        TracerEvent taskEvent = events.get(1);
        assertEquals(taskEvent.getEventType(), TASK_EVENT.name());
        assertEquals(taskEvent.getNodeId(), NODE_ID);
        assertEquals(taskEvent.getUri(), TEST_URI.getHost() + ":" + String.valueOf(TEST_URI.getPort()));
        assertEquals(taskEvent.getQueryId(), QUERY_ID);
        assertEquals(taskEvent.getStageId(), STAGE_ID);
        assertEquals(taskEvent.getTaskId(), TASK_ID);
        assertNull(taskEvent.getPipelineId());
        assertNull(taskEvent.getSplitId());
        assertNull(taskEvent.getOperatorId());
        assertEquals(taskEvent.getPayload(), jsonCodec.toJson(PAYLOAD));

        assertTrue(queryEvent.getTimeStamp() <= taskEvent.getTimeStamp());
    }
}
