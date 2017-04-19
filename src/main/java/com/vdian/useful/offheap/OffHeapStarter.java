package com.vdian.useful.offheap;

import com.vdian.useful.offheap.apps.AbstractAppInvoker;
import org.apache.commons.proxy.Interceptor;
import org.apache.commons.proxy.Invocation;
import org.apache.commons.proxy.ProxyFactory;
import org.apache.commons.proxy.factory.javassist.JavassistProxyFactory;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * * vm options
 * -Xmx512M
 * -XX:MaxDirectMemorySize=512M
 * -XX:+PrintGC
 * -XX:+UseConcMarkSweepGC
 * -XX:+CMSClassUnloadingEnabled
 * -XX:CMSInitiatingOccupancyFraction=80
 * -XX:+UseCMSInitiatingOccupancyOnly
 *
 * @author jifang
 * @since 2017/1/12 上午10:47.
 */
public class OffHeapStarter {

    private static final Map<String, Long> STATISTICS_MAP = new HashMap<>();

    public static void main(String[] args) throws IOException, IllegalAccessException, InstantiationException {
        Set<Class<?>> classes = PackageScanUtil.scanPackage("com.vdian.useful.offheap.apps");
        for (Class<?> clazz : classes) {
            AbstractAppInvoker invoker = createProxyInvoker(clazz.newInstance());
            invoker.invoke();

            //System.gc();
        }

        System.out.println("********************* statistics **********************");
        for (Map.Entry<String, Long> entry : STATISTICS_MAP.entrySet()) {
            System.out.println("method [" + entry.getKey() + "] total cost [" + entry.getValue() + "]ms");
        }
    }

    private static AbstractAppInvoker createProxyInvoker(Object invoker) {
        ProxyFactory factory = new JavassistProxyFactory();
        Class<?> superclass = invoker.getClass().getSuperclass();
        Object proxy = factory
                .createInterceptorProxy(invoker, new ProfileInterceptor(), new Class[]{superclass});
        return (AbstractAppInvoker) proxy;
    }

    private static class ProfileInterceptor implements Interceptor {

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            Class<?> clazz = invocation.getProxy().getClass();
            Method method = clazz.getMethod(invocation.getMethod().getName(), Object[].class);

            Object result = null;
            if (method.isAnnotationPresent(Test.class)
                    && method.getName().equals("invoke")) {

                String methodName = String.format("%s.%s", clazz.getSimpleName(), method.getName());
                System.out.println("method [" + methodName + "] start invoke");

                long start = System.currentTimeMillis();
                result = invocation.proceed();
                long cost = System.currentTimeMillis() - start;

                System.out.println("method [" + methodName + "] total cost [" + cost + "]ms");

                STATISTICS_MAP.put(methodName, cost);
            }

            return result;
        }
    }
}
