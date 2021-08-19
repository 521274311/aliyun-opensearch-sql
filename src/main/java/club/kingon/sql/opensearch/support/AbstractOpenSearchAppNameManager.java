package club.kingon.sql.opensearch.support;

import club.kingon.sql.opensearch.api.*;
import club.kingon.sql.opensearch.api.entry.*;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * @author dragons
 * @date 2021/8/12 11:29
 */
public abstract class AbstractOpenSearchAppNameManager extends AbstractOpenSearchClientManager {

    private final static Logger log = LoggerFactory.getLogger(AbstractOpenSearchAppNameManager.class);

    private final static String TIMER_THREAD_NAME_PREFIX = "opsh-apn-thread-";

    protected volatile long refreshMills = 60L * 60L * 1000L;

    private volatile AppName appNameStorage;

    private volatile String appName;

    protected final Object appNameSign = new Object();

    private Thread asyncRefreshInfoThread;

    public AbstractOpenSearchAppNameManager(String accessKey, String secret, Endpoint endpoint, String appName) {
        this(accessKey, secret, endpoint, false, appName);
    }

    public AbstractOpenSearchAppNameManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName) {
        super(accessKey, secret, endpoint, intranet);
        this.appName = appName;
        asyncRefreshInfo();
    }


    private void asyncRefreshInfo() {
        asyncRefreshInfoThread = new Thread(() -> {
            while (true) {
                try {
                    if (appName != null) {
                        CommonResponse resp = aliyunApiClient.execute(new OpenSearchAppNameDetailQueryApiRequest(appName));
                        OpenSearchAppNameDetailQueryApiData appNameData = JSON.parseObject(resp.getData(), OpenSearchAppNameDetailQueryApiData.class);
                        if (appNameStorage == null || appNameStorage.hashCode() != appNameData.getResult().hashCode()) {
                            appNameStorage = appNameData.getResult();
                        }
                        synchronized (appNameSign) {
                            appNameSign.notifyAll();
                        }
                    }
                } catch (ClientException e) {
                    log.error("async invoke opensearch app name struct api fail.", e);
                }
                try {
                    Thread.sleep(refreshMills);
                } catch (InterruptedException e) {
                    log.info("async refresh appname version info stop. message: {}", e.getMessage());
                    break;
                }
            }
        }, TIMER_THREAD_NAME_PREFIX);
        asyncRefreshInfoThread.start();
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public AppName getAppName() {
        return appNameStorage;
    }

    @Override
    public String getMasterVersion() {
        return appNameStorage == null ? null : appNameStorage.getCurrentVersion();
    }

    @Override
    public List<String> getVersions() {
        return appNameStorage == null ? Collections.emptyList() : appNameStorage.getVersions();
    }

    @Override
    public void close() {
        if (asyncRefreshInfoThread != null && asyncRefreshInfoThread.isAlive()) {
            asyncRefreshInfoThread.interrupt();
        }
    }
}
