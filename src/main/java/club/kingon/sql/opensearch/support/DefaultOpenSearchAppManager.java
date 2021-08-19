package club.kingon.sql.opensearch.support;

import club.kingon.sql.opensearch.api.Endpoint;

/**
 * @author dragons
 * @date 2021/8/12 16:02
 */
public class DefaultOpenSearchAppManager extends AbstractOpenSearchAppSchemaManager {

    public DefaultOpenSearchAppManager(String accessKey, String secret, Endpoint endpoint, String appName) {
        this(accessKey, secret, endpoint, false, appName);
    }

    public DefaultOpenSearchAppManager(String accessKey, String secret, Endpoint endpoint, String appName, long startWaitMills) {
        this(accessKey, secret, endpoint, false, appName, startWaitMills);
    }

    public DefaultOpenSearchAppManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName) {
        this(accessKey, secret, endpoint, intranet, appName, 500L);

    }

    public DefaultOpenSearchAppManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName, long startWaitMills) {
        super(accessKey, secret, endpoint, intranet, appName);
        sleep(startWaitMills);
    }

    private void sleep(long mills) {
        if (mills <= 0) return;
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
        }
    }
}