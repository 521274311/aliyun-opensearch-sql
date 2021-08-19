package club.kingon.sql.opensearch.support;

import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.api.Endpoint;
import club.kingon.sql.opensearch.api.OpenSearchAppNameDetailQueryApiRequest;
import club.kingon.sql.opensearch.api.entry.AppName;
import club.kingon.sql.opensearch.api.entry.OpenSearchAppNameDetailQueryApiData;
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
public abstract class AbstractOpenSearchAppNameManager extends AbstractOpenSearchRefreshRelyManager {

    private final static Logger log = LoggerFactory.getLogger(AbstractOpenSearchAppNameManager.class);

    private volatile AppName appNameStorage;

    private volatile String appName;

    protected final Object appNameSign = new Object();

    private boolean enableAppName = true;

    public AbstractOpenSearchAppNameManager(String accessKey, String secret, Endpoint endpoint, String appName) {
        this(accessKey, secret, endpoint, false, appName);
    }

    public AbstractOpenSearchAppNameManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName) {
        super(accessKey, secret, endpoint, intranet);
        this.appName = appName;
    }

    @Override
    protected void startAsyncTask() {
        addRefreshTaskFirst(Tuple2.of((t) -> enableAppName, (t) -> {
            if (appName != null) {
                CommonResponse resp = null;
                try {
                    resp = aliyunApiClient.execute(new OpenSearchAppNameDetailQueryApiRequest(appName));
                    OpenSearchAppNameDetailQueryApiData appNameData = JSON.parseObject(resp.getData(), OpenSearchAppNameDetailQueryApiData.class);
                    if (appNameStorage == null || appNameStorage.hashCode() != appNameData.getResult().hashCode()) {
                        appNameStorage = appNameData.getResult();
                    }
                } catch (ClientException e) {
                    log.error("async invoke opensearch app name struct api fail.", e);
                }
            }
        }), Tuple2.of(null, null));
        super.startAsyncTask();
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
}
