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

import io.jsonwebtoken.lang.Collections;
import io.prestosql.execution.QueryState;
import io.prestosql.execution.StageState;
import io.prestosql.execution.TaskState;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TracerActionType
{
    private TracerActionType()
    { }

    public static final String PLAN_QUERY_START = "PLAN_QUERY_START";
    public static final String PLAN_QUERY_END = "PLAN_QUERY_END";

    public static final String QUERY_STATE_CHANGE_QUEUED = "QUERY_STATE_CHANGE_" + QueryState.QUEUED.name();
    public static final String QUERY_STATE_CHANGE_WAITING_FOR_RESOURCES = "QUERY_STATE_CHANGE_" + QueryState.WAITING_FOR_RESOURCES.name();
    public static final String QUERY_STATE_CHANGE_DISPATCHING = "QUERY_STATE_CHANGE_" + QueryState.DISPATCHING.name();
    public static final String QUERY_STATE_CHANGE_PLANNING = "QUERY_STATE_CHANGE_" + QueryState.PLANNING.name();
    public static final String QUERY_STATE_CHANGE_STARTING = "QUERY_STATE_CHANGE_" + QueryState.STARTING.name();
    public static final String QUERY_STATE_CHANGE_RUNNING = "QUERY_STATE_CHANGE_" + QueryState.RUNNING.name();
    public static final String QUERY_STATE_CHANGE_FINISHING = "QUERY_STATE_CHANGE_" + QueryState.FINISHING.name();
    public static final String QUERY_STATE_CHANGE_FINISHED = "QUERY_STATE_CHANGE_" + QueryState.FINISHED.name();
    public static final String QUERY_STATE_CHANGE_FAILED = "QUERY_STATE_CHANGE_" + QueryState.FAILED.name();

    public static final String STAGE_STATE_CHANGE_PLANNED = "STAGE_STATE_CHANGE_" + StageState.PLANNED;
    public static final String STAGE_STATE_CHANGE_SCHEDULING = "STAGE_STATE_CHANGE_" + StageState.SCHEDULING;
    public static final String STAGE_STATE_CHANGE_SCHEDULING_SPLITS = "STAGE_STATE_CHANGE_" + StageState.SCHEDULING_SPLITS;
    public static final String STAGE_STATE_CHANGE_SCHEDULED = "STAGE_STATE_CHANGE_" + StageState.SCHEDULED;
    public static final String STAGE_STATE_CHANGE_RUNNING = "STAGE_STATE_CHANGE_" + StageState.RUNNING;
    public static final String STAGE_STATE_CHANGE_FINISHED = "STAGE_STATE_CHANGE_" + StageState.FINISHED;
    public static final String STAGE_STATE_CHANGE_CANCELED = "STAGE_STATE_CHANGE_" + StageState.CANCELED;
    public static final String STAGE_STATE_CHANGE_ABORTED = "STAGE_STATE_CHANGE_" + StageState.ABORTED;
    public static final String STAGE_STATE_CHANGE_FAILED = "STAGE_STATE_CHANGE_" + StageState.FAILED;

    public static final String ADD_SPLITS_TO_TASK = "ADD_SPLITS_TO_TASK";
    public static final String SCHEDULE_TASK_WITH_SPLITS = "SCHEDULE_TASK_WITH_SPLITS";
    public static final String SEND_UPDATE_TASK_REQUEST_START = "SEND_UPDATE_TASK_REQUEST_START";
    public static final String SEND_UPDATE_TASK_REQUEST_END = "SEND_UPDATE_TASK_REQUEST_END";

    public static final String MAKE_LOCAL_PLAN_START = "MAKE_LOCAL_PLAN_START";
    public static final String MAKE_LOCAL_PLAN_END = "MAKE_LOCAL_PLAN_END";

    public static final String TASK_STATE_CHANGE_PLANNED = "TASK_STATE_CHANGE_" + TaskState.PLANNED;
    public static final String TASK_STATE_CHANGE_RUNNING = "TASK_STATE_CHANGE_" + TaskState.RUNNING;
    public static final String TASK_STATE_CHANGE_FINISHED = "TASK_STATE_CHANGE_" + TaskState.FINISHED;
    public static final String TASK_STATE_CHANGE_CANCELED = "TASK_STATE_CHANGE_" + TaskState.CANCELED;
    public static final String TASK_STATE_CHANGE_ABORTED = "TASK_STATE_CHANGE_" + TaskState.ABORTED;
    public static final String TASK_STATE_CHANGE_FAILED = "TASK_STATE_CHANGE_" + TaskState.FAILED;

    public static final String CREATED_PIPELINE = "CREATED_PIPELINE";

    public static final String SPLIT_DRIVER_CREATED = "SPLIT_DRIVER_CREATED";
    public static final String SPLIT_SCHEDULED = "SPLIT_SCHEDULED";
    public static final String SPLIT_FINISHED = "SPLIT_FINISHED";
    public static final String SPLIT_STARTS_WAITING = "SPLIT_STARTS_WAITING";
    public static final String SPLIT_BLOCKED = "SPLIT_BLOCKED";
    public static final String SPLIT_UNBLOCKED = "SPLIT_UNBLOCKED";
    public static final String SPLIT_DESTROYED = "SPLIT_DESTROYED";

    public static final String CREATED_DRIVER = "CREATED_DRIVER";

    public static final String CREATED_OPERATOR = "CREATED_OPERATOR";
    public static final String ADD_SPLIT_TO_OPERATOR = "ADD_SPLIT_TO_OPERATOR";
    public static final String CREATE_PAGE_SOURCES_START = "CREATE_PAGE_SOURCES_START";
    public static final String CREATE_PAGE_SOURCES_END = "CREATE_PAGE_SOURCES_END";
    public static final String GET_NEXT_PAGE_START = "GET_NEXT_PAGE_START";
    public static final String GET_NEXT_PAGE_END = "GET_NEXT_PAGE_END";
    public static final String LOAD_PAGE_START = "LOAD_PAGE_START";
    public static final String LOAD_PAGE_END = "LOAD_PAGE_END";

    private static final Map<String, String> queryStateToActionMap = ((List<QueryState>) Collections.arrayToList(QueryState.values())).stream().collect(Collectors.toMap(x -> x.name(), x -> "QUERY_STATE_CHANGE_" + x.name()));
    private static final Map<String, String> stageStateToActionMap = ((List<StageState>) Collections.arrayToList(StageState.values())).stream().collect(Collectors.toMap(x -> x.name(), x -> "STAGE_STATE_CHANGE_" + x.name()));
    private static final Map<String, String> taskStateToActionMap = ((List<TaskState>) Collections.arrayToList(TaskState.values())).stream().collect(Collectors.toMap(x -> x.name(), x -> "TASK_STATE_CHANGE_" + x.name()));

    public static String getQueryChangeActionType(QueryState state)
    {
        return queryStateToActionMap.get(state.name());
    }

    public static String getStageChangeActionType(StageState state)
    {
        return stageStateToActionMap.get(state.name());
    }

    public static String getTaskChangeActionType(TaskState state)
    {
        return taskStateToActionMap.get(state.name());
    }
}
