package com.vdian.se.apps;

import com.vdian.se.domain.FeedDO;
import org.ehcache.Cache;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.ResourcePools;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.junit.Test;

/**
 * @author jifang
 * @since 2017/1/12 下午2:57.
 */
public class EhcacheApp extends AbstractAppInvoker {

    private static Cache<String, FeedDO> cache;

    static {
        ResourcePools resourcePools = ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(1000, EntryUnit.ENTRIES)
                .offheap(480, MemoryUnit.MB)
                .build();

        CacheConfiguration<String, FeedDO> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, FeedDO.class, resourcePools)
                .build();

        cache = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("cacher", configuration)
                .build(true)
                .getCache("cacher", String.class, FeedDO.class);

    }

    //@Test
    @Override
    public void invoke(Object... param) {
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            FeedDO feedDO = createFeed(i, key, System.currentTimeMillis());
            cache.put(key, feedDO);
        }

        System.out.println("write down");
        // read
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            Object o = cache.get(key);
            checkValid(o);

            if (i % 10000 == 0) {
                System.out.println("read " + i);
            }
        }
    }
}
