package com.vdian.se.serialize;

/**
 * @author zhoupan@weidian.com
 * @since 16/7/8.
 */
public interface IObjectSerializer {

    <T> byte[] serialize(T obj);

    <T> T deserialize(byte[] bytes);
}
