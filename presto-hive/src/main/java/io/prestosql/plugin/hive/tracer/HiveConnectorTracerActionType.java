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
package io.prestosql.plugin.hive.tracer;

public final class HiveConnectorTracerActionType
{
    private HiveConnectorTracerActionType()
    { }

    public static final String LIST_SYMLINK_TEXT_INPUT_FILE_STATES_START = "LIST_SYMLINK_TEXT_INPUT_FILE_STATES_START";
    public static final String LIST_SYMLINK_TEXT_INPUT_FILE_STATES_END = "LIST_SYMLINK_TEXT_INPUT_FILE_STATES_END";
    public static final String LIST_FILE_STATES_START = "LIST_FILE_STATES_START";
    public static final String LIST_FILE_STATES_END = "LIST_FILE_STATES_END";
    public static final String LOAD_SPLIT_START = "LOAD_SPLIT_START";
    public static final String LOAD_SPLIT_END = "LOAD_SPLIT_END";

    public static final String GET_FILE_SYSTEM_START = "GET_FILE_SYSTEM_START";
    public static final String GET_FILE_SYSTEM_END = "GET_FILE_SYSTEM_END";
    public static final String OPEN_FILE_SYSTEM_INPUT_STREAM_START = "OPEN_FILE_SYSTEM_INPUT_STREAM_START";
    public static final String OPEN_FILE_SYSTEM_INPUT_STREAM_END = "OPEN_FILE_SYSTEM_INPUT_STREAM_END";
}
