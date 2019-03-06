package com.xingzy;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.xingzy.annotation.Subscribe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author roy.xing
 * @date 2019/3/6
 */
public class EventBus {

    private static String TAG = EventBus.class.getSimpleName();

    private static volatile EventBus instance;
    private HashMap<Object, List<SubscribeMethod>> cacheMap;
    private Handler handler;
    private ExecutorService executorService;

    private EventBus() {
        cacheMap = new HashMap<>();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    public static EventBus getDefault() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    public void register(Object obj) {
        //将对象中的方法添加到EventBus中来管理
        List<SubscribeMethod> list = cacheMap.get(obj);
        if (list == null) {
            list = findSubscribeMethods(obj);
            cacheMap.put(obj, list);
        }
    }

    /**
     * 遍历对象中所有EventBus的回调方法
     */
    private List<SubscribeMethod> findSubscribeMethods(Object obj) {
        List<SubscribeMethod> list = new ArrayList<>();
        Class<?> clazz = obj.getClass();

        while (clazz != null) {
            //判断当前是否是系统类 如果是系统类 则退出循环
            String name = clazz.getName();
            if (name.startsWith("java.") || name.startsWith("javax")) {
                break;
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                Subscribe subscribe = method.getAnnotation(Subscribe.class);
                if (subscribe == null) {
                    continue;
                }
                //获取方法中的参数 判断是否唯一
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    Log.e(TAG, "回调方法中只能传递一个参数");
                }
                SubscribeMethod subscribeMethod = new SubscribeMethod(method, subscribe.threadMode(), parameterTypes[0]);
                list.add(subscribeMethod);
            }
            //寻找父类
            clazz = clazz.getSuperclass();
        }
        return list;
    }

    /**
     * @param type 回调方法参数
     */
    public void post(final Object type) {
        Set<Object> set = cacheMap.keySet();
        Iterator<Object> iterator = set.iterator();

        while (iterator.hasNext()) {
            final Object obj = iterator.next();
            List<SubscribeMethod> subscribeMethods = cacheMap.get(obj);
            for (final SubscribeMethod subscribeMethod : subscribeMethods) {
                //简单理解：对比两个类是否一致 a(subscribeMethod.getType())对象所有对应的类信息是否是b(type.getClass())
                //对象所对应的类信息的父类或者父接口
                if (subscribeMethod.getType().isAssignableFrom(type.getClass())) {
                    ThreadMode threadMode = subscribeMethod.getThreadMode();
                    switch (threadMode) {
                        case MAIN:
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                invoke(subscribeMethod, obj, type);
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(subscribeMethod, obj, type);
                                    }
                                });
                            }
                            break;
                        case BACKGROUND:
                            //主线程到子线程
                            if (Looper.myLooper() == Looper.getMainLooper()) {
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(subscribeMethod, obj, type);
                                    }
                                });
                            } else {
                                invoke(subscribeMethod, obj, type);
                            }
                            break;
                        default:
                            break;
                    }

                }
            }
        }
    }

    private void invoke(SubscribeMethod subscribeMethod, Object obj, Object type) {
        Method method = subscribeMethod.getMethod();
        try {
            method.invoke(obj, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unRegister(Object obj) {
        cacheMap.remove(obj);
    }
}
