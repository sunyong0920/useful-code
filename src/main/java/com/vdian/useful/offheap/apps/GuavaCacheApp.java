package com.vdian.useful.offheap.apps;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.vdian.useful.offheap.domain.FeedDO;
import org.junit.Test;

/**
 * @author jifang
 * @since 2017/1/12 下午2:40.
 */
public class GuavaCacheApp extends AbstractAppInvoker {

    private static final LoadingCache<String, FeedDO> guavaCache;

    static {
        guavaCache = CacheBuilder
                .newBuilder()
                .build(new CacheLoader<String, FeedDO>() {
                    @Override
                    public FeedDO load(String key) throws Exception {
                        return null;
                    }
                });
    }


    @Test
    @Override
    public void invoke(Object... param) {
        // write
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            FeedDO feedDO = createFeed(i, key, System.currentTimeMillis());

            guavaCache.put(key, feedDO);
        }

        System.out.println("write down");
        // read
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            FeedDO feedDO = guavaCache.getUnchecked(key);
            checkValid(feedDO);

            if (i % 10000 == 0) {
                System.out.println("read " + i);
            }
        }
    }
}
