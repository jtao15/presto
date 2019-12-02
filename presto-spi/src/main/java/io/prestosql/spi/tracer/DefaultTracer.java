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
package io.prestosql.spi.tracer;

import io.prestosql.spi.eventlistener.TracerEvent;

import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

// Tracer keeps track of Presto execution unit contexts and sends debugging purpose events via its emitter.
// Users should not build complex metrics based on the action events.
public class DefaultTracer
        implements Tracer
{
    private final boolean enabled;
    private final Consumer<TracerEvent> emitter;
    private final String queryId;
    private final Optional<String> stageId;
    private final Optional<String> taskId;
    private final Optional<String> pipelineId;
    private final Optional<String> splitId;
    private final Optional<String> operatorId;
    // presto worker node emitting tracer events
    private final Optional<String> nodeId;

    private DefaultTracer(boolean isEnabled, Consumer<TracerEvent> emitter, String queryId, Optional<String> stageId, Optional<String> taskId, Optional<String> pipelineId, Optional<String> splitId, Optional<String> operatorId, Optional<String> nodeId)
    {
        this.enabled = isEnabled;
        this.emitter = requireNonNull(emitter, "emitter is null");
        this.queryId = requireNonNull(queryId, "queryId is null");
        this.stageId = requireNonNull(stageId, "stageId is null");
        this.taskId = requireNonNull(taskId, "taskId is null");
        this.pipelineId = requireNonNull(pipelineId, "pipelineId is null");
        this.splitId = requireNonNull(splitId, "splitId is null");
        this.operatorId = requireNonNull(operatorId, "operatorId is null");
        this.nodeId = requireNonNull(nodeId, "nodeId is null");
    }

    public static Tracer createBasicTracer(Consumer<TracerEvent> emitter, String queryId, boolean enabled)
    {
        return (new Builder(emitter, queryId, enabled)).build();
    }

    @Override
    public Tracer newTracerWithStageId(String stageId)
    {
        return (new Builder(this)).withStageId(stageId).build();
    }

    @Override
    public Tracer newTracerWithTaskId(String taskId)
    {
        return (new Builder(this)).withTaskId(taskId).build();
    }

    @Override
    public Tracer newTracerWithPipelineId(String pipelineId)
    {
        return (new Builder(this)).withPipelineId(pipelineId).build();
    }

    @Override
    public Tracer newTracerWithSplitId(String splitId)
    {
        return (new Builder(this)).withSplitId(splitId).build();
    }

    @Override
    public Tracer newTracerWithOperatorId(String operatorId)
    {
        return (new Builder(this)).withOperatorId(operatorId).build();
    }

    @Override
    public Tracer newTracerWithNodeId(String nodeId)
    {
        return (new Builder(this)).withNodeId(nodeId).build();
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public Consumer<TracerEvent> getEmitter()
    {
        return emitter;
    }

    public String getQueryId()
    {
        return queryId;
    }

    public Optional<String> getStageId()
    {
        return stageId;
    }

    public Optional<String> getTaskId()
    {
        return taskId;
    }

    public Optional<String> getPipelineId()
    {
        return pipelineId;
    }

    public Optional<String> getSplitId()
    {
        return splitId;
    }

    public Optional<String> getOperatorId()
    {
        return operatorId;
    }

    public Optional<String> getNodeId()
    {
        return nodeId;
    }

    @Override
    public void emitEvent(TracerEventType eventType, PayloadBuilder payload)
    {
        if (!enabled) {
            return;
        }

        TracerEvent event = new TracerEvent(
                queryId,
                stageId.orElse(null),
                taskId.orElse(null),
                pipelineId.orElse(null),
                splitId.orElse(null),
                operatorId.orElse(null),
                nodeId.orElse(null),
                Thread.currentThread().getName(),
                System.currentTimeMillis(),
                eventType.name(),
                payload == null ? null : payload.getPayload());

        emitter.accept(event);
    }

    private static class Builder
    {
        private final boolean enabled;
        private final Consumer<TracerEvent> emitter;
        private String queryId;
        private Optional<String> stageId = Optional.empty();
        private Optional<String> taskId = Optional.empty();
        private Optional<String> pipelineId = Optional.empty();
        private Optional<String> splitId = Optional.empty();
        private Optional<String> operatorId = Optional.empty();
        private Optional<String> nodeId = Optional.empty();

        public Builder(Consumer<TracerEvent> emitter, String queryId, boolean isEnabled)
        {
            this.emitter = requireNonNull(emitter);
            this.queryId = requireNonNull(queryId);
            this.enabled = isEnabled;
        }

        public Builder(DefaultTracer tracer)
        {
            requireNonNull(tracer);
            this.enabled = tracer.isEnabled();
            this.emitter = tracer.getEmitter();
            this.queryId = tracer.getQueryId();
            this.stageId = tracer.getStageId();
            this.taskId = tracer.getTaskId();
            this.pipelineId = tracer.getPipelineId();
            this.splitId = tracer.getSplitId();
            this.nodeId = tracer.getNodeId();
        }

        public Builder withStageId(String stageId)
        {
            requireNonNull(stageId);
            this.stageId = Optional.of(stageId);
            return this;
        }

        public Builder withTaskId(String taskId)
        {
            requireNonNull(taskId);
            this.taskId = Optional.of(taskId);
            return this;
        }

        public Builder withPipelineId(String pipelineId)
        {
            requireNonNull(pipelineId);
            this.pipelineId = Optional.of(pipelineId);
            return this;
        }

        public Builder withSplitId(String splitId)
        {
            requireNonNull(splitId);
            this.splitId = Optional.of(splitId);
            return this;
        }

        public Builder withOperatorId(String operatorId)
        {
            requireNonNull(operatorId);
            this.operatorId = Optional.of(operatorId);
            return this;
        }

        public Builder withNodeId(String nodeId)
        {
            requireNonNull(nodeId);
            this.nodeId = Optional.of(nodeId);
            return this;
        }

        public Tracer build()
        {
            return new DefaultTracer(enabled, emitter, queryId, stageId, taskId, pipelineId, splitId, operatorId, nodeId);
        }
    }
}
