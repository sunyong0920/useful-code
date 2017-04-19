package com.vdian.useful.offheap.apps;

import com.vdian.useful.offheap.domain.FeedDO;
import com.vdian.useful.offheap.serialize.Hessian2Serializer;
import com.vdian.useful.offheap.serialize.IObjectSerializer;
import org.junit.Test;
import redis.clients.jedis.Jedis;

/**
 * @author jifang
 * @since 2017/1/12 下午3:19.
 */
public class RemoteRedisApp extends AbstractAppInvoker {

    private static final Jedis cache = new Jedis("devgroup", 6379);

    private static final IObjectSerializer serializer = new Hessian2Serializer();

    @Test
    @Override
    public void invoke(Object... param) {
        // write
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            FeedDO feedDO = createFeed(i, key, System.currentTimeMillis());

            byte[] value = serializer.serialize(feedDO);
            cache.set(key.getBytes(), value);

            if (i % 1000 == 0) {
                System.out.println("write " + i);
            }
        }

        System.out.println("write down");
        // read
        for (int i = 0; i < SIZE; ++i) {
            String key = String.format("key_%s", i);
            byte[] value = cache.get(key.getBytes());
            FeedDO feedDO = serializer.deserialize(value);
            checkValid(feedDO);

            if (i % 1000 == 0) {
                System.out.println("read " + i);
            }
        }
    }
}
