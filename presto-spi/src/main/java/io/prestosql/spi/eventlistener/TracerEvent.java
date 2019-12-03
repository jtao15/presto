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
package io.prestosql.spi.eventlistener;

import static java.util.Objects.requireNonNull;

public class TracerEvent
{
    private final String nodeId;
    private final String uri;
    private final String queryId;
    private final String stageId;
    private final String taskId;
    private final String pipelineId;
    private final String splitId;
    private final String operatorId;

    private final String threadName;

    //time of the source node
    private final long timestampEpochMillis;

    private final String eventType;

    private final String payload;

    public TracerEvent(
            String nodeId,
            String uri,
            String queryId,
            String stageId,
            String taskId,
            String pipelineId,
            String splitId,
            String operatorId,
            String threadName,
            long timeMillis,
            String eventType,
            String payload)
    {
        this.nodeId = requireNonNull(nodeId, "nodeId is null");
        this.uri = requireNonNull(uri, "uri is null");
        this.queryId = requireNonNull(queryId, "queryId is null");
        this.stageId = stageId;
        this.taskId = taskId;
        this.pipelineId = pipelineId;
        this.splitId = splitId;
        this.operatorId = operatorId;
        this.threadName = requireNonNull(threadName, "threadName is null");
        if (timeMillis < 0) {
            throw new IllegalArgumentException("Invalid timestamp: " + timeMillis);
        }
        this.timestampEpochMillis = timeMillis;
        this.eventType = requireNonNull(eventType, "actionType is null");
        this.payload = payload;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public String getUri()
    {
        return uri;
    }

    public String getQueryId()
    {
        return queryId;
    }

    public String getStageId()
    {
        return stageId;
    }

    public String getTaskId()
    {
        return taskId;
    }

    public String getPipelineId()
    {
        return pipelineId;
    }

    public String getSplitId()
    {
        return splitId;
    }

    public String getOperatorId()
    {
        return operatorId;
    }

    public String getThreadName()
    {
        return threadName;
    }

    public long getTimeStamp()
    {
        return timestampEpochMillis;
    }

    public String getEventType()
    {
        return eventType;
    }

    public String getPayload()
    {
        return payload;
    }

    @Override
    public String toString()
    {
        return
                "nodeId: " + nodeId +
                ", uri: " + uri +
                ", queryId: " + queryId +
                ", stageId: " + (stageId == null ? "" : stageId) +
                ", taskId: " + (taskId == null ? "" : taskId) +
                ", pipelineId: " + (pipelineId == null ? "" : pipelineId) +
                ", splitId: " + (splitId == null ? "" : splitId) +
                ", operatorId: " + (operatorId == null ? "" : operatorId) +
                ", threadName: " + (threadName == null ? "" : threadName) +
                ", timeStamp: " + timestampEpochMillis + " millis" +
                ", action: " + eventType +
                ", payload: " + payload;
    }
}
