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

public class NoOpTracer
        implements Tracer
{
    @Override
    public Tracer newTracerWithStageId(String stageId)
    {
        return this;
    }

    @Override
    public Tracer newTracerWithTaskId(String taskId)
    {
        return this;
    }

    @Override
    public Tracer newTracerWithPipelineId(String pipelineId)
    {
        return this;
    }

    @Override
    public Tracer newTracerWithSplitId(String splitId)
    {
        return this;
    }

    @Override
    public Tracer newTracerWithOperatorId(String operatorId)
    {
        return this;
    }

    @Override
    public Tracer newTracerWithNodeId(String nodeId)
    {
        return this;
    }

    @Override
    public void emitEvent(TracerEventType actionType, PayloadBuilder payload)
    { }

    @Override
    public boolean isEnabled()
    {
        return false;
    }
}
