package com.vdian.useful.jbox;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vdian.vbox.utils.Collections3;
import com.vdian.vitamin.client.VitaminClient;
import com.vdian.vitamin.client.listener.HandleListener;
import com.vdian.vitamin.common.model.NodeDO;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurablePropertyResolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author jifang
 * @since 2017/4/5 上午10:35.
 * <p/>
 * 从Spring 3.1开始建议使用PropertySourcesPlaceholderConfigurer装配properties, 因为它能够基于Spring Environment及其属性源来解析占位符.
 */
public class PropertySourcesPlaceholderWithVitamin extends PropertySourcesPlaceholderConfigurer implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertySourcesPlaceholderWithVitamin.class);

    private static final ConcurrentMap<String, String> CONFIG_PROPERTIES = new ConcurrentHashMap<>();

    private static final Map<Class, Class> primitiveTypeMap = new ConcurrentHashMap<>(14);

    static {
        primitiveTypeMap.put(byte.class, Byte.class);
        primitiveTypeMap.put(Byte.class, Byte.class);
        primitiveTypeMap.put(short.class, Short.class);
        primitiveTypeMap.put(Short.class, Short.class);
        primitiveTypeMap.put(int.class, Integer.class);
        primitiveTypeMap.put(Integer.class, Integer.class);
        primitiveTypeMap.put(long.class, Long.class);
        primitiveTypeMap.put(Long.class, Long.class);
        primitiveTypeMap.put(float.class, Float.class);
        primitiveTypeMap.put(Float.class, Float.class);
        primitiveTypeMap.put(double.class, Double.class);
        primitiveTypeMap.put(Double.class, Double.class);
        primitiveTypeMap.put(boolean.class, Boolean.class);
        primitiveTypeMap.put(Boolean.class, Boolean.class);
    }

    private ConfigurableListableBeanFactory beanFactory;

    private Map<String, String> vitaminPropertiesTmp = new HashMap<>();

    private Multimap<String, Pair<Field, Object>> beanWithValueAnnotationMap = HashMultimap.create();

    private boolean needVitamin = true;

    private String groupId = "properties";

    private String serviceId;

    private HandleListener listener = new HandleListener() {
        @Override
        public void handle(List<NodeDO> nodes) {
            handleNodeChange(nodes);
        }
    };

    public static ConcurrentMap<String, String> getProperties() {
        return CONFIG_PROPERTIES;
    }

    public static String getProperty(String key) {
        return CONFIG_PROPERTIES.get(key);
    }

    public void setNeedVitamin(boolean needVitamin) {
        this.needVitamin = needVitamin;
    }

    public void setGroupId(String groupId) {
        if (!Strings.isNullOrEmpty(groupId)) {
            this.groupId = groupId;
        }
    }

    public void setServiceId(String serviceId) {
        if (!Strings.isNullOrEmpty(serviceId)) {
            this.serviceId = serviceId;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        if (this.needVitamin) {
            if (!Strings.isNullOrEmpty(this.serviceId)) {
                Map<String, String> nodeValueMap = VitaminClient.lookup(this.groupId, this.serviceId, listener);
                initVitaminProperties(this.serviceId, nodeValueMap);
            } else {
                List<String> serviceIds = VitaminClient.queryServiceList(this.groupId);
                for (String serviceId : serviceIds) {
                    Map<String, String> nodeValueMap = VitaminClient.lookup(this.groupId, serviceId, listener);
                    initVitaminProperties(serviceId, nodeValueMap);
                }
            }
        }
    }

    private void initVitaminProperties(String serviceId, Map<String, String> nodeValueMap) {
        for (Map.Entry<String, String> entry : nodeValueMap.entrySet()) {
            // combine property key
            String nodeKey = entry.getKey();
            String propertyKey = combinePropertyKey(serviceId, nodeKey);

            String propertyValue = entry.getValue();

            vitaminPropertiesTmp.put(propertyKey, propertyValue);
        }
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, ConfigurablePropertyResolver propertyResolver) throws BeansException {
        this.beanFactory = beanFactoryToProcess;
        super.processProperties(beanFactoryToProcess, propertyResolver);
    }

    @Override
    protected Properties mergeProperties() throws IOException {
        Properties springProperties = super.mergeProperties();

        storeProperties(springProperties);
        storeProperties(this.vitaminPropertiesTmp);

        springProperties.putAll(this.vitaminPropertiesTmp);

        return springProperties;
    }

    private void storeProperties(Map<?, ?> map) {
        if (!Collections3.isNullOrEmpty(map)) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                CONFIG_PROPERTIES.put(key, value);
            }
        }
    }

    /*  -------------------------------------   */
    /*  ------- Handle Node Changes ---------   */
    /*  -------------------------------------   */

    private void handleNodeChange(List<NodeDO> nodes) {
        initBeansMap(this.beanFactory);
        for (NodeDO nodeDO : nodes) {
            String nodeValue = nodeDO.getNodeValue();
            String propertyKey = combinePropertyKey(nodeDO.getServiceId(), nodeDO.getNodeKey());
            // 更新map
            CONFIG_PROPERTIES.put(propertyKey, nodeValue);

            Collection<Pair<Field, Object>> filedWithBeans = this.beanWithValueAnnotationMap.get(propertyKey);
            if (!Collections3.isNullOrEmpty(filedWithBeans)) {
                for (Pair<Field, Object> pair : filedWithBeans) {
                    Field field = pair.getLeft();
                    Object beanInstance = pair.getRight();
                    Object filedValue = convertTypeValue(nodeValue, field.getType());

                    try {
                        field.set(beanInstance, filedValue);
                    } catch (IllegalAccessException ignored) {
                        // 不可能发生
                    }

                    LOGGER.info("class: {}`s instance field: {} value is change to {}", beanInstance.getClass().getName(),
                            field.getName(), filedValue);
                }
            } else {
                LOGGER.warn("propertyKey: {} have not found relation bean, value: {}", propertyKey, nodeValue);
            }
        }
    }

    private void initBeansMap(ConfigurableListableBeanFactory beanFactory) {
        if (beanWithValueAnnotationMap.isEmpty()) {
            String[] beanNames = beanFactory.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                Object bean = beanFactory.getBean(beanName);
                initBeanMap(bean);
            }
        }
    }

    private void initBeanMap(Object beanInstance) {
        Field[] fields = beanInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Value.class)) {
                String valueMapKey = getValueAnnotationValue(field);
                Pair<Field, Object> pair = Pair.of(field, beanInstance);

                beanWithValueAnnotationMap.put(valueMapKey, pair);
            }
        }
    }

    private String getValueAnnotationValue(Field field) {
        makeAccessible(field);

        String value = field.getAnnotation(Value.class).value();
        if (value.startsWith("${") && value.endsWith("}")) {
            return value.substring("${".length(), value.length() - 1);
        } else {
            throw new RuntimeException("@Value annotation need \"${[serviceId].[nodeId]}\" config in the value() property");
        }
    }

    // 仅支持八种基本类型和String
    private Object convertTypeValue(String value, Class type) {
        Object instance = null;
        Class<?> primitiveType = primitiveTypeMap.get(type);
        if (primitiveType != null) {
            try {
                instance = primitiveType.getMethod("valueOf", String.class).invoke(null, value);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            }
        } else if (type == CharEncoding.class || type == char.class) {
            instance = value.charAt(0);
        } else {
            instance = value;
        }

        return instance;
    }

    private String combinePropertyKey(String serviceId, String nodeKey) {
        if (!Strings.isNullOrEmpty(this.serviceId)) {
            return nodeKey;
        } else {
            return String.format("%s.%s", serviceId, nodeKey);
        }
    }

    /**
     * From spring-core
     * Make the given field accessible, explicitly setting it accessible if
     * necessary. The {@code setAccessible(true)} method is only called
     * when actually necessary, to avoid unnecessary conflicts with a JVM
     * SecurityManager (if active).
     *
     * @param field the field to make accessible
     * @see Field#setAccessible
     */
    private void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }
}
