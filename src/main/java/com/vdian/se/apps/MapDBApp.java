package com.vdian.se.apps;

import com.vdian.se.domain.FeedDO;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

/**
 * @author jifang
 * @since 2017/1/8 下午7:39.
 */
public class MapDBApp extends AbstractAppInvoker {

    private static final HTreeMap<String, FeedDO> mapDBCache;

    static {
        mapDBCache = DBMaker.hashMapSegmentedMemoryDirect()
                .expireMaxSize(SIZE)
                .make();
    }

    //@Test
    @Override
    public void invoke(Object... param) {

        for (int i = 0; i < SIZE; ++i) {
            String key = "key-" + i;
            FeedDO feed = createFeed(i, key, System.currentTimeMillis());

            mapDBCache.put(key, feed);
        }

        System.out.println("write down");
        for (int i = 0; i < SIZE; ++i) {
            String key = "key-" + i;
            FeedDO feedDO = mapDBCache.get(key);
            checkValid(feedDO);

            if (i % 10000 == 0) {
                System.out.println("read " + i);
            }
        }
    }
}
