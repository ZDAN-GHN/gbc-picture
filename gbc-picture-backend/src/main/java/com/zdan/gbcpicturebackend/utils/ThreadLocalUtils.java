package com.zdan.gbcpicturebackend.utils;

/**
 * ThreadLocal工具类，用于在当前线程中存储各种数据
 */
public class ThreadLocalUtils {

    private static final ThreadLocal<Object> THREAD_LOCAL = new ThreadLocal<>();

    private ThreadLocalUtils() {
    }

    /**
     * 设置值到当前线程的ThreadLocal
     *
     * @param value 要设置的值
     */
    public static void set(Object value) {
        THREAD_LOCAL.set(value);
    }

    /**
     * 获取当前线程的ThreadLocal中的值
     *
     * @return 当前线程的ThreadLocal中的值
     */
    public static <T> T get() {
        return (T) THREAD_LOCAL.get();
    }

    /**
     * 获取当前线程的ThreadLocal中的值，如果为空则使用默认值并设置
     *
     * @param defaultValue 默认值
     * @return 当前线程的ThreadLocal中的值或默认值
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(T defaultValue) {
        Object value = THREAD_LOCAL.get();
        if (value == null) {
            THREAD_LOCAL.set(defaultValue);
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * 移除当前线程的ThreadLocal中的值
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }

    /**
     * 判断当前线程的ThreadLocal中是否有值
     *
     * @return 如果当前线程的ThreadLocal中有值则返回true，否则返回false
     */
    public static boolean hasValue() {
        return THREAD_LOCAL.get() != null;
    }

    /**
     * 清空当前线程的ThreadLocal中的值
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
