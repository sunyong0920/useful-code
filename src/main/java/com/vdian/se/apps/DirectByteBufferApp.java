package com.vdian.se.apps;

import com.vdian.se.domain.FeedDO;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 16/10/9 下午6:05.
 */
public class DirectByteBufferApp extends AbstractAppInvoker {

    //@Test
    @Override
    public void invoke(Object... param) {
        Map<String, FeedDO> map = createInHeapMap(SIZE);

        // move in off-heap
        byte[] bytes = serializer.serialize(map);
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        map = null;
        bytes = null;
        System.out.println("write down");
        // move out from off-heap
        byte[] offHeapBytes = new byte[buffer.limit()];
        buffer.get(offHeapBytes);
        Map<String, FeedDO> deserMap = serializer.deserialize(offHeapBytes);
        for (int i = 0; i < SIZE; ++i) {
            String key = "key-" + i;
            FeedDO feedDO = deserMap.get(key);
            checkValid(feedDO);

            if (i % 10000 == 0) {
                System.out.println("read " + i);
            }
        }

        free(buffer);
    }

    private Map<String, FeedDO> createInHeapMap(int size) {
        long createTime = System.currentTimeMillis();

        Map<String, FeedDO> map = new ConcurrentHashMap<>(size);
        for (int i = 0; i < size; ++i) {
            String key = "key-" + i;
            FeedDO value = createFeed(i, key, createTime);
            map.put(key, value);
        }

        return map;
    }
}

