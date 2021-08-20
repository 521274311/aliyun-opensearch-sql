package club.kingon.sql.opensearch.support;

import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.api.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * OpenSearch定时刷新任务管理器
 * 子类通过重写 addTask 方法，调用 addAsyncTask 添加定时刷新任务
 * @author dragons
 * @date 2021/8/19 12:40
 */
public abstract class AbstractOpenSearchRefreshTaskManager extends AbstractOpenSearchClientManager {

    private final static Logger log = LoggerFactory.getLogger(AbstractOpenSearchRefreshTaskManager.class);

    private final List<Tuple2<Predicate, Consumer>> refreshTaskFunctions = new ArrayList<>();

    private final List<Tuple2<?, ?>> refreshTaskDatas = new ArrayList<>();

    protected long refreshMills = 30 * 60 * 1000L;

    private Thread asyncTaskThread;

    private final Object lock = new Object();

    public AbstractOpenSearchRefreshTaskManager(String accessKey, String secret, Endpoint endpoint) {
        this(accessKey, secret, endpoint, false);
    }

    public AbstractOpenSearchRefreshTaskManager(String accessKey, String secret, Endpoint endpoint, boolean intranet) {
        super(accessKey, secret, endpoint, intranet);
        startRefreshTask();
    }

    public AbstractOpenSearchRefreshTaskManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, long refreshMills) {
        super(accessKey, secret, endpoint, intranet);
        this.refreshMills = refreshMills;
        startRefreshTask();
    }

    private void startRefreshTask() {
        // 递归注入循环任务
        injectTask();
        asyncTaskThread = new Thread(() -> {
            while (true) {
                try {
                    if (refreshTaskFunctions.size() != refreshTaskDatas.size()) {
                        continue;
                    }

                    for (int i = 0; i < refreshTaskFunctions.size(); i++) {
                        if (refreshTaskFunctions.get(i).t1.test(refreshTaskDatas.get(i).t1)) {
                            refreshTaskFunctions.get(i).t2.accept(refreshTaskDatas.get(i).t2);
                        }
                    }
                    synchronized (lock) {
                        lock.wait(refreshMills);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        asyncTaskThread.start();
    }

    private void injectTask() {
        Class<?> invokeClass = getClass();
        Class<?> superClass = getClass();
        MethodType methodType = MethodType.methodType(void.class);
        try {
            Field IMPL_LOOKUP = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            IMPL_LOOKUP.setAccessible(true);
            while (superClass != null) {
                try {
                    superClass.getDeclaredMethod("addTask");
                    ((MethodHandles.Lookup)IMPL_LOOKUP.get(null))
                        .findSpecial(superClass, "addTask", methodType, invokeClass)
                        .bindTo(this)
                        .invoke();
                    if (log.isDebugEnabled()) {
                        log.info("inject class: {} method named addTask.", superClass.getName());
                    }
                } catch (Throwable e) {
                } finally {
                    invokeClass = superClass;
                    superClass = superClass.getSuperclass();
                }
            }
        } catch (Throwable e) {
            log.error("inject task fail.", e);
        }
    }

    protected void addRefreshTask(Tuple2<Predicate, Consumer> function, Tuple2<?, ?> data) {
        if (refreshTaskFunctions.size() == 0 && refreshTaskDatas.size() == 0) {
            refreshTaskFunctions.add(function);
            refreshTaskDatas.add(data);
        } else {
            refreshTaskFunctions.add(0, function);
            refreshTaskDatas.add(0, data);
        }
    }

    protected abstract void addTask();

    protected void runRefreshTaskOnce() {
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public void close() {
        if (asyncTaskThread != null && asyncTaskThread.isAlive()) {
            asyncTaskThread.interrupt();
        }
    }
}
