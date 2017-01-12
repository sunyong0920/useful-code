package com.vdian.se.apps;

import com.google.common.base.Strings;
import com.vdian.se.domain.FeedDO;
import com.vdian.se.serialize.Hessian2Serializer;
import com.vdian.se.serialize.IObjectSerializer;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * @author jifang
 * @since 2017/1/12 上午10:48.
 */
public abstract class AbstractAppInvoker {

    protected static final int SIZE = 170_0000;

    protected static final IObjectSerializer serializer = new Hessian2Serializer();

    protected static FeedDO createFeed(long id, String userId, long createTime) {

        return new FeedDO(id, userId, (int) id, userId + "_" + id, createTime);
    }

    protected static void free(ByteBuffer byteBuffer) {
        if (byteBuffer.isDirect()) {
            ((DirectBuffer) byteBuffer).cleaner().clean();
        }
    }

    protected static void checkValid(Object obj) {
        if (obj == null) {
            throw new RuntimeException("cache invalid");
        }
    }

    protected static void sleep(int time, String beforeMsg) {
        if (!Strings.isNullOrEmpty(beforeMsg)) {
            System.out.println(beforeMsg);
        }

        try {
            Thread.sleep(time);
        } catch (InterruptedException ignored) {
            // no op
        }
    }


    /**
     * 供子类继承 & 外界调用
     *
     * @param param
     */
    public abstract void invoke(Object... param);
}
