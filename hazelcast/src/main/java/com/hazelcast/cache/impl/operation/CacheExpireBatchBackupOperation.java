/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.cache.impl.operation;

import com.hazelcast.cache.impl.CacheDataSerializerHook;
import com.hazelcast.cache.impl.record.CacheRecord;
import com.hazelcast.internal.eviction.ExpiredKey;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.ExceptionAction;
import com.hazelcast.spi.exception.WrongTargetException;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Used to transfer expired keys from owner replica to backup replicas.
 */
public class CacheExpireBatchBackupOperation extends CacheOperation {

    private Collection<ExpiredKey> expiredKeys;
    private int ownerPartitionEntryCount;

    public CacheExpireBatchBackupOperation() {
    }

    public CacheExpireBatchBackupOperation(String name, Collection<ExpiredKey> expiredKeys, int ownerPartitionEntryCount) {
        super(name, true);
        this.expiredKeys = expiredKeys;
        this.ownerPartitionEntryCount = ownerPartitionEntryCount;
    }

    @Override
    public void run() {
        for (ExpiredKey expiredKey : expiredKeys) {
            evictIfSame(expiredKey);
        }

        int diff = recordStore.size() - ownerPartitionEntryCount;
        for (int i = 0; i < diff; i++) {
            recordStore.evictOneEntry();
        }
        assert recordStore.size() == ownerPartitionEntryCount;
    }

    @Override
    public ExceptionAction onInvocationException(Throwable throwable) {
        if (throwable instanceof WrongTargetException) {
            if (((WrongTargetException) throwable).getTarget() == null) {
                // If there isn't any address of backup replica, no need to retry this operation.
                return ExceptionAction.THROW_EXCEPTION;
            }
        }

        return super.onInvocationException(throwable);
    }

    protected void evictIfSame(ExpiredKey key) {
        if (recordStore == null) {
            return;
        }
        CacheRecord record = recordStore.getRecord(key.getKey());
        if (record != null && record.getCreationTime() == key.getCreationTime()) {
            recordStore.removeRecord(key.getKey());
        }
    }

    @Override
    public int getId() {
        return CacheDataSerializerHook.EXPIRE_BATCH_BACKUP;
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        out.writeInt(expiredKeys.size());
        for (ExpiredKey expiredKey : expiredKeys) {
            out.writeData(expiredKey.getKey());
            out.writeLong(expiredKey.getCreationTime());
        }
        out.writeInt(ownerPartitionEntryCount);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        int size = in.readInt();
        expiredKeys = new LinkedList<ExpiredKey>();
        for (int i = 0; i < size; i++) {
            expiredKeys.add(new ExpiredKey(in.readData(), in.readLong()));
        }
        ownerPartitionEntryCount = in.readInt();
    }
}
