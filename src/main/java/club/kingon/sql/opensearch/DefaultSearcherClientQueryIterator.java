package club.kingon.sql.opensearch;

import club.kingon.sql.opensearch.util.Constants;
import club.kingon.sql.opensearch.util.OpenSearchBuilderUtil;
import club.kingon.sql.opensearch.util.OpenSearchConverter;
import club.kingon.sql.opensearch.util.ResponseConstants;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.aliyun.opensearch.SearcherClient;
import com.aliyun.opensearch.sdk.dependencies.org.json.JSONArray;
import com.aliyun.opensearch.sdk.dependencies.org.json.JSONException;
import com.aliyun.opensearch.sdk.dependencies.org.json.JSONObject;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchClientException;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchException;
import com.aliyun.opensearch.sdk.generated.search.*;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 完成一次查询迭代
 * 默认查询迭代器实现
 * 非线程安全
 * @see OpenSearchDqlException 所有方法均可能出现OpenSearchDqlException
 * @author dragons
 * @date 2020/12/23 18:29
 */
public class DefaultSearcherClientQueryIterator extends AbstractSearcherClientQueryIterator
        implements SearcherClientQueryIterator {

    private final static Logger log = LoggerFactory.getLogger(DefaultSearcherClientQueryIterator.class);

    private final SearcherClient searcherClient;
    private final List<String> appNames;
    private int offset;
    private int count;
    private final List<String> fetchField;
    private final String query;
    private final String filter;
    private Set<Distinct> distincts;
    private final Set<Aggregate> aggregates;
    private final Sort sort;
    private DeepPaging deepPaging;
    private SearchQueryModeEnum queryMode = SearchQueryModeEnum.HIT;
    private SearchResult result;
    private int retry = 1;
    private long retryTimeInterval = 100L;
    private long pagingInterval = 100L;
    private int batch = Constants.MAX_ONE_HIT;
    private final List<String> queryProcessorNames;
    private final Rank rank;
    private final String kvpairs;

    private boolean alreadyExplain = false;
    private JSONArray items = null;

    public DefaultSearcherClientQueryIterator(SearcherClient client, String sql) {
        super();
        searcherClient = client;
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, Constants.MYSQL_DB_TYPE);
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        statement.accept(visitor);
        MySqlSelectQueryBlock block = ((MySqlSelectQueryBlock)((SQLSelectStatement) statement).getSelect().getQuery());
        appNames = OpenSearchConverter.explainFrom(visitor);
        fetchField = OpenSearchConverter.explainFetchField(block);
        Tuple2<Tuple2<String, String>, Map<String, Object>> queryAndFilterAndParams = OpenSearchConverter.explainWhere(block.getWhere());
        query = queryAndFilterAndParams.t1.t1;
        filter = queryAndFilterAndParams.t1.t2;
        queryProcessorNames = (List<String>) queryAndFilterAndParams.t2.get(Constants.QUERY_PROCESSOR_NAMES);
        rank = OpenSearchConverter.expainRank(queryAndFilterAndParams.t2);
        kvpairs = (String) queryAndFilterAndParams.t2.get(Constants.DEFAULT_KVPAIRS);
        distincts = OpenSearchConverter.explainDistinct(block);
        aggregates = OpenSearchConverter.explainAggregate(block, visitor);
        sort = OpenSearchConverter.explainSort(block);
        Tuple2<Integer, Integer> offsetAndCount = OpenSearchConverter.explainStartAndHit(block.getLimit());
        if (offsetAndCount.t2 == null || offsetAndCount.t2 > Constants.MAX_ALL_HIT) {
            queryMode = SearchQueryModeEnum.SCROLL;
            count = Integer.MAX_VALUE;
            deepPaging = new DeepPaging();
            deepPaging.setScrollExpire(Constants.FIVE_MINUTE_ABBREVIATION);
        } else {
            offset = offsetAndCount.t1;
            count = offsetAndCount.t2;
        }
    }

    /**
     *
     * @throws JSONException
     */
    @Override
    public boolean hasNext() {
        int retry = this.retry;
        if (result != null) {
            return true;
        }
        if (count == 0) {
            return false;
        }
        int num = count > batch ? batch : count;
        OpenSearchBuilderUtil.SearchParamsBuilder searchParamsBuilder = OpenSearchBuilderUtil.searchParamsBuilder(
                OpenSearchBuilderUtil.configBuilder(appNames, offset, num, fetchField).kvpairs(kvpairs).build(), query)
                .filter(filter)
                // 支持设置qp
                .queryProcessorNames(queryProcessorNames)
                // 支持粗排、精排表达式
                .rank(rank)
                .sort(sort);
        while (retry-- >= 0) {
            try {
                if (queryMode == SearchQueryModeEnum.HIT) {
                    // 添加去重、聚合
                    searchParamsBuilder.distincts(distincts).aggregates(aggregates);
                } else if (queryMode == SearchQueryModeEnum.SCROLL) {
                    // 添加滚动查询
                    searchParamsBuilder.deepPaging(deepPaging);
                }
                SearchParams searchParams = searchParamsBuilder.build();
                if (log.isDebugEnabled()) {
                    log.debug("SearchParams: {}", searchParams);
                }
                SearchResult searchResult = searcherClient.execute(searchParams);
                if (log.isDebugEnabled()) {
                    log.debug("SearchResult: {}", searchResult);
                }
                if (searchResult == null || searchResult.getResult() == null) {
                    if (log.isErrorEnabled()) {
                        log.error("search result is null" );
                    }
                    return false;
                }
                JSONObject resultJson = new JSONObject(searchResult.getResult());
                String status = resultJson.getString(ResponseConstants.STATUS);
                if (queryMode == SearchQueryModeEnum.SCROLL) {
                    String scrollId = resultJson.getJSONObject(ResponseConstants.RESULT).getString(ResponseConstants.SCROLLID);
                    if (deepPaging.getScrollId() == null) {
                        deepPaging.setScrollId(scrollId);
                        searchResult = searcherClient.execute(searchParams);
                        resultJson = new JSONObject(searchResult.getResult());
                        status = resultJson.getString(ResponseConstants.STATUS);
                        if (ResponseConstants.STATUS_OK.equals(status)) {
                            scrollId = resultJson.getJSONObject(ResponseConstants.RESULT).getString(ResponseConstants.SCROLLID);
                        } else {
                            deepPaging.setScrollId(null);
                            if (log.isErrorEnabled()) {
                                log.error("scroll mode request fail, info: {}", searchResult.getResult());
                            }
                        }
                    }
                    deepPaging.setScrollId(scrollId);
                }
                if (ResponseConstants.STATUS_OK.equals(status)) {
                    if (!resultJson.has(ResponseConstants.RESULT)) {
                        if (log.isDebugEnabled()) {
                            log.debug("status is ok, but hasnot result param. info: {}", searchResult.getResult());
                        }
                    }
                    JSONArray items = resultJson.getJSONObject(ResponseConstants.RESULT).getJSONArray(ResponseConstants.RESULT_ITEMS);
                    if (items.length() > 0) {
                        result = searchResult;
                        if (queryMode == SearchQueryModeEnum.HIT) {
                            offset += num;
                        }
                        count -= num;
                        // 重置状态
                        reset();
                        return true;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("search status is ok, but result length is zero");
                    }
                    return false;
                }
                if (log.isDebugEnabled()) {
                    log.debug("search status is fail, info: {}", searchResult.getResult());
                }
            } catch (OpenSearchException | OpenSearchClientException e) {
                if (log.isErrorEnabled()) {
                    log.error("OpenSearch exception", e);
                }
                if (retry < 0) {
                    throw new OpenSearchDqlException(e);
                }
            }
            try {
                Thread.sleep(retryTimeInterval);
            } catch (InterruptedException e) {
            }
        }
        return false;
    }

    @Override
    public boolean hasNextOne() {
        return items != null && items.length() > 0 || hasNext();
    }

    /**
     * Nullable
     * @throws NullPointerException
     */
    @Override
    public SearchResult next() {
        waitPagingInterval();
        SearchResult res = result;
        result = null;
        return res;
    }

    /**
     * Nullable
     * @throws NullPointerException
     * @throws JSONException
     */
    @Override
    public JSONObject nextOne() {
        waitPagingInterval();
        // 尚未解析
        if ((items == null || items.length() == 0) && !alreadyExplain) {
            items = (new JSONObject(result.getResult())).getJSONObject(ResponseConstants.RESULT).getJSONArray(ResponseConstants.RESULT_ITEMS);
            alreadyExplain = true;
        }
        if (items == null || items.length() == 0) {
            return null;
        }
        JSONObject item = items.getJSONObject(0).getJSONObject(Constants.FIELDS);
        items.remove(0);
        result = null;
        return item;
    }

    private void waitPagingInterval() {
        try {
            Thread.sleep(pagingInterval);
        } catch (InterruptedException e) {
        }
    }

    private void reset() {
        alreadyExplain = false;
        items = null;
    }

    public int getBatch() {
        return batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public long getPagingInterval() {
        return pagingInterval;
    }

    public void setPagingInterval(long pagingInterval) {
        this.pagingInterval = pagingInterval;
    }

    public void setScrollExpr(String expr) {
        deepPaging.setScrollExpire(expr);
    }

    public String getScrollExpr() {
        return deepPaging.getScrollExpire();
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public long getRetryTimeInterval() {
        return retryTimeInterval;
    }

    public void setRetryTimeInterval(long retryTimeInterval) {
        this.retryTimeInterval = retryTimeInterval;
    }

    public List<String> getAppNames() {
        return appNames;
    }

    public int getOffset() {
        return offset;
    }

    public int getCount() {
        return count;
    }

    public List<String> getFetchField() {
        return fetchField;
    }

    public String getQuery() {
        return query;
    }

    public String getFilter() {
        return filter;
    }

    public void setDistincts(Set<Distinct> distincts) {
        this.distincts = distincts;
    }

    public Set<Distinct> getDistincts() {
        return distincts;
    }

    public Set<Aggregate> getAggregates() {
        return aggregates;
    }

    public Sort getSort() {
        return sort;
    }

    public DeepPaging getDeepPaging() {
        return deepPaging;
    }

    public SearchQueryModeEnum getQueryMode() {
        return queryMode;
    }
}
