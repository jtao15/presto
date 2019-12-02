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

import io.prestosql.Session;
import io.prestosql.eventlistener.EventListenerManager;
import io.prestosql.spi.tracer.DefaultTracer;
import io.prestosql.spi.tracer.Tracer;

import javax.inject.Inject;

import static io.prestosql.SystemSessionProperties.isQueryDebuggingTracerEnabled;
import static java.util.Objects.requireNonNull;

public final class DefaultTracerFactory
        implements TracerFactory
{
    private final EventListenerManager eventListenerManager;

    @Inject
    public DefaultTracerFactory(EventListenerManager eventListenerManager)
    {
        this.eventListenerManager = requireNonNull(eventListenerManager, "eventListenerManger is null");
    }

    @Override
    public Tracer createBasicTracer(String queryId, Session session)
    {
        return DefaultTracer.createBasicTracer(event -> eventListenerManager.tracerEventOccurred(event), queryId, isQueryDebuggingTracerEnabled(session));
    }
}
