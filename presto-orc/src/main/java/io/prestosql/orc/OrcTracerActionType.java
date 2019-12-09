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

public final class OrcTracerActionType
{
    private OrcTracerActionType()
    { }

    public static final String READ_ORC_TAIL_START = "READ_ORC_TAIL_START";
    public static final String READ_ORC_TAIL_END = "READ_ORC_TAIL_END";
    public static final String READ_ORC_COMPLETE_FOOTER_START = "READ_ORC_COMPLETE_FOOTER_START";
    public static final String READ_ORC_COMPLETE_FOOTER_END = "READ_ORC_COMPLETE_FOOTER_END";
    public static final String READ_STRIPE_START = "READ_STRIPE_START";
    public static final String READ_STRIPE_END = "READ_STRIPE_END";
}
