/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.map.record;

import com.hazelcast.nio.serialization.Data;

public interface Record<V> {

    Data getKey();

    V getValue();

    void setValue(V value);

    void invalidate();

    RecordStatistics getStatistics();

    void setStatistics(RecordStatistics stats);

    void onAccess();

    void onUpdate();

    void onStore();

    long getCost();

    long getVersion();

    void setVersion(long version);
}
