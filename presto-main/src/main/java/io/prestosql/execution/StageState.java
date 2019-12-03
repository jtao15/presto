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
package io.prestosql.execution;

import io.prestosql.spi.tracer.TracerEventType;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_ABORTED;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_CANCELED;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_FAILED;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_FINISHED;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_PLANNED;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_RUNNING;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_SCHEDULED;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_SCHEDULING;
import static io.prestosql.spi.tracer.TracerEventType.STAGE_STATE_CHANGE_SCHEDULING_SPLITS;

public enum StageState
{
    /**
     * Stage is planned but has not been scheduled yet.  A stage will
     * be in the planned state until, the dependencies of the stage
     * have begun producing output.
     */
    PLANNED(false, false)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_PLANNED;
        }
    },
    /**
     * Stage tasks are being scheduled on nodes.
     */
    SCHEDULING(false, false)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_SCHEDULING;
        }
    },
    /**
     * All stage tasks have been scheduled, but splits are still being scheduled.
     */
    SCHEDULING_SPLITS(false, false)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_SCHEDULING_SPLITS;
        }
    },
    /**
     * Stage has been scheduled on nodes and ready to execute, but all tasks are still queued.
     */
    SCHEDULED(false, false)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_SCHEDULED;
        }
    },
    /**
     * Stage is running.
     */
    RUNNING(false, false)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_RUNNING;
        }
    },
    /**
     * Stage has finished executing and all output has been consumed.
     */
    FINISHED(true, false)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_FINISHED;
        }
    },
    /**
     * Stage was canceled by a user.
     */
    CANCELED(true, false)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_CANCELED;
        }
    },
    /**
     * Stage was aborted due to a failure in the query.  The failure
     * was not in this stage.
     */
    ABORTED(true, true)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_ABORTED;
        }
    },
    /**
     * Stage execution failed.
     */
    FAILED(true, true)
    {
        @Override
        public TracerEventType toTracerEventType()
        {
            return STAGE_STATE_CHANGE_FAILED;
        }
    };

    public static final Set<StageState> TERMINAL_STAGE_STATES = Stream.of(StageState.values()).filter(StageState::isDone).collect(toImmutableSet());

    private final boolean doneState;
    private final boolean failureState;

    StageState(boolean doneState, boolean failureState)
    {
        checkArgument(!failureState || doneState, "%s is a non-done failure state", name());
        this.doneState = doneState;
        this.failureState = failureState;
    }

    public TracerEventType toTracerEventType()
    {
        throw new NotImplementedException();
    }

    /**
     * Is this a terminal state.
     */
    public boolean isDone()
    {
        return doneState;
    }

    /**
     * Is this a non-success terminal state.
     */
    public boolean isFailure()
    {
        return failureState;
    }
}
