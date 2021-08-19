package club.kingon.sql.opensearch.support;

import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.api.Endpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author dragons
 * @date 2021/8/19 12:40
 */
public abstract class AbstractOpenSearchRefreshRelyManager extends AbstractOpenSearchClientManager {

    private final List<Tuple2<Predicate, Consumer>> refreshTaskFunctions = new ArrayList<>();

    private final List<Tuple2<?, ?>> refreshTaskDatas = new ArrayList<>();

    protected long refreshMills = 30 * 60 * 1000L;

    private Thread asyncTaskThread;

    public AbstractOpenSearchRefreshRelyManager(String accessKey, String secret, Endpoint endpoint) {
        this(accessKey, secret, endpoint, false);
    }

    public AbstractOpenSearchRefreshRelyManager(String accessKey, String secret, Endpoint endpoint, boolean intranet) {
        super(accessKey, secret, endpoint, intranet);
        startAsyncTask();
    }

    protected void startAsyncTask() {
        asyncTaskThread = new Thread(() -> {
            while (true) {
                if (refreshTaskFunctions.size() != refreshTaskDatas.size()) {
                    continue;
                }

                for (int i = 0; i < refreshTaskFunctions.size(); i++) {
                    if (refreshTaskFunctions.get(i).t1.test(refreshTaskDatas.get(i).t1)) {
                        refreshTaskFunctions.get(i).t2.accept(refreshTaskDatas.get(i).t2);
                    }
                }

                try {
                    Thread.sleep(refreshMills);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        asyncTaskThread.start();
    }

    protected void addRefreshTaskFirst(Tuple2<Predicate, Consumer> function, Tuple2<?, ?> data) {
        if (refreshTaskFunctions.size() == 0 && refreshTaskDatas.size() == 0) {
            refreshTaskFunctions.add(function);
            refreshTaskDatas.add(data);
        } else {
            refreshTaskFunctions.add(0, function);
            refreshTaskDatas.add(0, data);
        }
    }

    protected void addRefreshTask(Tuple2<Predicate, Consumer> function, Tuple2<?, ?> data) {
        refreshTaskFunctions.add(function);
        refreshTaskDatas.add(data);
    }

    @Override
    public void close() {
        if (asyncTaskThread != null && asyncTaskThread.isAlive()) {
            asyncTaskThread.interrupt();
        }
    }
}
