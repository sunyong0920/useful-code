package com.vdian.useful.offheap;

import com.vdian.useful.offheap.apps.AbstractAppInvoker;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jifang
 * @since 2016/10/27 上午9:38.
 */
public class PackageScanUtil {

    private static final String CLASS_SUFFIX = ".class";

    private static final String FILE_PROTOCOL = "file";

    public static Set<Class<?>> scanPackage(String packageName) throws IOException {

        Set<Class<?>> classes = new HashSet<>();
        String packageDir = packageName.replace('.', '/');
        Enumeration<URL> packageResources = Thread.currentThread().getContextClassLoader().getResources(packageDir);
        while (packageResources.hasMoreElements()) {
            URL packageResource = packageResources.nextElement();

            String protocol = packageResource.getProtocol();
            // 只扫描项目内class
            if (FILE_PROTOCOL.equals(protocol)) {
                String packageDirPath = URLDecoder.decode(packageResource.getPath(), "UTF-8");
                scanProjectPackage(packageName, packageDirPath, classes);
            }
        }

        return classes;
    }

    private static void scanProjectPackage(String packageName, String packageDirPath, Set<Class<?>> classes) {

        File packageDirFile = new File(packageDirPath);
        if (packageDirFile.exists() && packageDirFile.isDirectory()) {

            File[] subFiles = packageDirFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() || pathname.getName().endsWith(CLASS_SUFFIX);
                }
            });

            for (File subFile : subFiles) {
                if (!subFile.isDirectory()) {
                    String className = trimClassSuffix(subFile.getName());
                    String classNameWithPackage = packageName + "." + className;

                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(classNameWithPackage);
                    } catch (ClassNotFoundException e) {
                        // ignore
                    }
                    assert clazz != null;

                    Class<?> superclass = clazz.getSuperclass();
                    if (superclass == AbstractAppInvoker.class) {
                        classes.add(clazz);
                    }
                }
            }
        }
    }

    // trim .class suffix
    private static String trimClassSuffix(String classNameWithSuffix) {
        int endIndex = classNameWithSuffix.length() - CLASS_SUFFIX.length();
        return classNameWithSuffix.substring(0, endIndex);
    }
}