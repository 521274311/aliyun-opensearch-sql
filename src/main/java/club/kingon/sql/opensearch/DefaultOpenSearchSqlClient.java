package club.kingon.sql.opensearch;

import club.kingon.sql.opensearch.api.Endpoint;
import club.kingon.sql.opensearch.support.DefaultOpenSearchManager;
import com.aliyun.opensearch.SearcherClient;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * @author dragons
 * @date 2021/8/12 16:39
 */
public class DefaultOpenSearchSqlClient implements OpenSearchSqlClient {

    private final static Logger log = LoggerFactory.getLogger(DefaultOpenSearchSqlClient.class);

    private OpenSearchManager openSearchManager;

    private SearcherClient searcherClient;

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint) {
        this(accessKey, secret, endpoint, null);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet) {
        this(accessKey, secret, endpoint, intranet, null);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, String appName) {
        this(accessKey, secret, endpoint, appName, 2000L);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, String appName, long startWaitMills) {
        this(accessKey, secret, endpoint, false, appName, startWaitMills);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName) {
        this(accessKey, secret, endpoint, intranet, appName, 2000L);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName, long startWaitMills) {
        openSearchManager = new DefaultOpenSearchManager(accessKey, secret,endpoint, intranet, appName, startWaitMills);
        searcherClient = new SearcherClient(openSearchManager.getOpenSearchClient());
    }

    @Override
    public Iterator<SearchResult> query(String sql) {
        return new DefaultSearcherClientQueryIterator(searcherClient, sql, openSearchManager);
    }



    @Override
    public void close() {
        openSearchManager.close();
        log.info("OpenSearchSqlClient close resource success.");
    }
}
