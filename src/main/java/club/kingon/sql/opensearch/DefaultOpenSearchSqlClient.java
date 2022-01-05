package club.kingon.sql.opensearch;

import club.kingon.sql.opensearch.api.Endpoint;
import club.kingon.sql.opensearch.parser.DefaultOpenSearchSQLParser;
import club.kingon.sql.opensearch.parser.OpenSearchSQLParser;
import club.kingon.sql.opensearch.parser.entry.OpenSearchDataOperationEntry;
import club.kingon.sql.opensearch.parser.entry.OpenSearchQueryEntry;
import club.kingon.sql.opensearch.support.DefaultOpenSearchAppManager;
import com.aliyun.opensearch.DocumentClient;
import com.aliyun.opensearch.SearcherClient;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchClientException;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchException;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchResult;
import com.aliyun.opensearch.sdk.generated.search.Aggregate;
import com.aliyun.opensearch.sdk.generated.search.Distinct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * @author dragons
 * @date 2021/8/12 16:39
 */
public class DefaultOpenSearchSqlClient implements OpenSearchSqlClient {

    private final static Logger log = LoggerFactory.getLogger(DefaultOpenSearchSqlClient.class);

    private OpenSearchManager openSearchManager;

    private SearcherClient searcherClient;

    private DocumentClient documentClient;

    private OpenSearchSQLParser sqlParser;

    private SearchQueryModeEnum defaultSearchMode;

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint) {
        this(accessKey, secret, endpoint, null);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, int connectionTimeout, int readTimeout) {
        this(accessKey, secret, endpoint, null, connectionTimeout, readTimeout);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet) {
        this(accessKey, secret, endpoint, intranet, null);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, int connectionTimeout, int readTimeout) {
        this(accessKey, secret, endpoint, intranet, null, connectionTimeout, readTimeout);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, String appName) {
        this(accessKey, secret, endpoint, appName, 2000L);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, String appName, int connectionTimeout, int readTimeout) {
        this(accessKey, secret, endpoint, appName, 2000L, connectionTimeout, readTimeout);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, String appName, long startWaitMills) {
        this(accessKey, secret, endpoint, false, appName, startWaitMills);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, String appName, long startWaitMills, int connectionTimeout, int readTimeout) {
        this(accessKey, secret, endpoint, false, appName, startWaitMills, connectionTimeout, readTimeout);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName) {
        this(accessKey, secret, endpoint, intranet, appName, 2000L);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName, int connectionTimeout, int readTimeout) {
        this(accessKey, secret, endpoint, intranet, appName, 2000L, connectionTimeout, readTimeout);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName, long startWaitMills) {
        this(accessKey, secret, endpoint, intranet, appName, startWaitMills, true);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName, long startWaitMills, int connectionTimeout, int readTimeout) {
        this(accessKey, secret, endpoint, intranet, appName, startWaitMills, true, connectionTimeout, readTimeout);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName, long startWaitMills, boolean enableManagement) {
        this(accessKey, secret, endpoint, intranet, appName, startWaitMills, enableManagement, -1 , -1);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet,
                                      String appName, long startWaitMills, boolean enableManagement,
                                      int connectionTimeout, int readTimeout) {
        this(accessKey, secret, endpoint, intranet, appName, startWaitMills, enableManagement, connectionTimeout, readTimeout, SearchQueryModeEnum.HIT);
    }

    public DefaultOpenSearchSqlClient(String accessKey, String secret, Endpoint endpoint, boolean intranet,
                                      String appName, long startWaitMills, boolean enableManagement,
                                      int connectionTimeout, int readTimeout, SearchQueryModeEnum defaultSearchMode) {
        openSearchManager = new DefaultOpenSearchAppManager(accessKey, secret,endpoint, intranet, appName, startWaitMills, enableManagement, connectionTimeout, readTimeout);
        searcherClient = new SearcherClient(openSearchManager.getOpenSearchClient());
        documentClient = new DocumentClient(openSearchManager.getOpenSearchClient());
        sqlParser = new DefaultOpenSearchSQLParser(openSearchManager, this);
        // 默认搜索模式, 若默认搜索模式为HIT, 当使用者未添加limit限制时, 将会填充limit 10, 若搜索模式为SCROLL未添加limit限制将依旧触发scroll滚动
        this.defaultSearchMode = defaultSearchMode;
    }


    @Override
    public OpenSearchQueryIterator query(String sql, Set<Distinct> distincts) {
        return query(sql, distincts, null);
    }

    @Override
    public OpenSearchQueryIterator query(String sql, Set<Distinct> distincts, Set<Aggregate> aggregates) {
        OpenSearchQueryEntry config = sqlParser.parse(sql);
        // 支持distinct参数化注入
        if (distincts != null && !distincts.isEmpty()) {
            config.setDistincts(distincts);
        }
        // 支持aggregate参数化注入
        if (aggregates != null && !aggregates.isEmpty()) {
            config.setAggregates(aggregates);
        }
        // defaultSearchModel检查及修正
        if (SearchQueryModeEnum.HIT.equals(defaultSearchMode) && SearchQueryModeEnum.SCROLL.equals(config.getQueryMode())) {
            config.setQueryMode(SearchQueryModeEnum.HIT);
            config.setDeepPaging(null);
            config.setCount(10);
        }
        return new DefaultOpenSearchQueryIterator(searcherClient, config);
    }

    @Override
    public OpenSearchResult insert(String sql) {
        OpenSearchDataOperationEntry entry = sqlParser.parse(sql);
        for (Map<String, Object> data : entry.getData()) {
            documentClient.add(data);
        }
        try {
            return documentClient.commit(entry.getAppName(), entry.getTableName());
        } catch (OpenSearchException | OpenSearchClientException e) {
            throw new OpenSearchDqlException("exception occurred when inserting data. insert sql:" + sql, e);
        }
    }

    @Override
    public OpenSearchResult update(String sql) {
        OpenSearchDataOperationEntry entry = sqlParser.parse(sql);
        for (Map<String, Object> data : entry.getData()) {
            documentClient.update(data);
        }
        try {
            return documentClient.commit(entry.getAppName(), entry.getTableName());
        } catch (OpenSearchException | OpenSearchClientException e) {
            throw new OpenSearchDqlException("exception occurred when inserting data. insert sql:" + sql, e);
        }
    }

    @Override
    public OpenSearchResult delete(String sql) {
        OpenSearchDataOperationEntry entry = sqlParser.parse(sql);
        for (Map<String, Object> data : entry.getData()) {
            documentClient.remove(data);
        }
        try {
            return documentClient.commit(entry.getAppName(), entry.getTableName());
        } catch (OpenSearchException | OpenSearchClientException e) {
            throw new OpenSearchDqlException("exception occurred when inserting data. insert sql:" + sql, e);
        }
    }


    @Override
    public void close() {
        openSearchManager.close();
        log.info("OpenSearchSqlClient close resource success.");
    }
}
