package com.vdian.useful.offheap.apps;

import com.vdian.useful.offheap.domain.FeedDO;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jifang
 * @since 2017/1/12 下午2:28.
 */
public class ConcurrentHashMapApp extends AbstractAppInvoker {

    private static final Map<String, FeedDO> cache = new ConcurrentHashMap<>();

    @Test
    @Override
    public void invoke(Object... param) {

        // write
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            FeedDO feedDO = createFeed(i, key, System.currentTimeMillis());
            cache.put(key, feedDO);
        }

        System.out.println("write down");
        // read
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            FeedDO feedDO = cache.get(key);
            checkValid(feedDO);

            if (i % 10000 == 0) {
                System.out.println("read " + i);
            }
        }
    }
}
