package com.vdian.useful.offheap.domain;

import java.io.Serializable;

/**
 * @author jifang
 * @since 2017/1/8 下午4:04.
 */
public class FeedDO implements Serializable {

    private static final long serialVersionUID = -7825025501241247517L;

    private long id;

    private String userId;

    private int type;

    private String content;

    private long createTime;

    public FeedDO(long id, String userId, int type, String content, long createTime) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.createTime = createTime;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
