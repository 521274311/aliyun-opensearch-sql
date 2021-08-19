package club.kingon.sql.opensearch;

import club.kingon.sql.opensearch.api.Endpoint;
import club.kingon.sql.opensearch.parser.DefaultOpenSearchSQLParser;
import club.kingon.sql.opensearch.parser.OpenSearchQueryEntry;
import club.kingon.sql.opensearch.parser.OpenSearchSQLParser;
import club.kingon.sql.opensearch.support.DefaultOpenSearchAppManager;
import com.aliyun.opensearch.SearcherClient;
import com.aliyun.opensearch.sdk.generated.search.Distinct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author dragons
 * @date 2021/8/12 16:39
 */
public class DefaultOpenSearchSqlClient implements OpenSearchSqlClient {

    private final static Logger log = LoggerFactory.getLogger(DefaultOpenSearchSqlClient.class);

    private OpenSearchManager openSearchManager;

    private SearcherClient searcherClient;

    private OpenSearchSQLParser sqlParser;

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
        openSearchManager = new DefaultOpenSearchAppManager(accessKey, secret,endpoint, intranet, appName, startWaitMills);
        searcherClient = new SearcherClient(openSearchManager.getOpenSearchClient());
        sqlParser = new DefaultOpenSearchSQLParser(openSearchManager);
    }

    @Override
    public OpenSearchQueryIterator query(String sql, Set<Distinct> distincts) {
        OpenSearchQueryEntry config = sqlParser.parse(sql);
        // 支持distinct参数化注入
        if (distincts != null && !distincts.isEmpty()) {
            config.setDistincts(distincts);
        }
        return new DefaultOpenSearchQueryIterator(searcherClient, config);
    }



    @Override
    public void close() {
        openSearchManager.close();
        log.info("OpenSearchSqlClient close resource success.");
    }
}
