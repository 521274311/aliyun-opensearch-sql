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
import com.aliyun.opensearch.sdk.dependencies.org.json.JSONObject;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchClientException;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchException;
import com.aliyun.opensearch.sdk.generated.search.*;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * 完成一次查询迭代
 * 默认查询迭代器实现
 * @see OpenSearchDqlException 所有方法均可能出现OpenSearchDqlException
 * @author dragons
 * @date 2020/12/23 18:29
 */
public class DefaultSearcherClientQueryIterator extends AbstractSearcherClientQueryIterator
        implements SearcherClientQueryIterator {

    private final static Logger log = LoggerFactory.getLogger(DefaultSearcherClientQueryIterator.class);

    private SearcherClient searcherClient;
    private List<String> appNames;
    private int offset;
    private int count;
    private List<String> fetchField;
    private String query;
    private String filter;
    private Set<Distinct> distincts;
    private Set<Aggregate> aggregates;
    private Sort sort;
    private DeepPaging deepPaging;
    private SearchQueryModeEnum queryMode = SearchQueryModeEnum.HIT;
    private String result;
    private int retry = 1;
    private long retryTimeInterval = 100L;
    private long pagingInterval = 100L;

    public DefaultSearcherClientQueryIterator(SearcherClient client, String sql) {
        super();
        searcherClient = client;
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, Constants.MYSQL_DB_TYPE);
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        statement.accept(visitor);
        MySqlSelectQueryBlock block = ((MySqlSelectQueryBlock)((SQLSelectStatement) statement).getSelect().getQuery());
        appNames = OpenSearchConverter.explainAppNames(visitor);
        fetchField = OpenSearchConverter.explainFetchField(block);
        Tuple2<String, String> queryAndFilter = OpenSearchConverter.explainQueryAndFilter((SQLBinaryOpExpr) block.getWhere());
        query = queryAndFilter.t1;
        filter = queryAndFilter.t2;
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

    @Override
    public boolean hasNext() {
        int retry = this.retry;
        if (result != null) {
            return true;
        }
        if (count == 0) {
            return false;
        }
        int num = count > Constants.MAX_ONE_HIT ? Constants.MAX_ONE_HIT : count;
        OpenSearchBuilderUtil.SearchParamsBuilder searchParamsBuilder = OpenSearchBuilderUtil.searchParamsBuilder(
                OpenSearchBuilderUtil.configBuilder(appNames, offset, num, fetchField).build(), query)
                .filter(filter)
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
                            if (log.isWarnEnabled()) {
                                log.warn("scroll mode request fail, info: {}", searchResult.getResult());
                            }
                        }
                    }
                    deepPaging.setScrollId(scrollId);
                }
                JSONArray items = resultJson.getJSONObject(ResponseConstants.RESULT).getJSONArray(ResponseConstants.RESULT_ITEMS);
                if (ResponseConstants.STATUS_OK.equals(status)) {
                    if (items.length() > 0) {
                        result = searchResult.getResult();
                        if (queryMode == SearchQueryModeEnum.HIT) {
                            offset += num;
                        }
                        count -= num;
                        return true;
                    }
                    return false;
                }
            } catch (OpenSearchException | OpenSearchClientException e) {
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

    /**
     * Nullable
     */
    @Override
    public String next() {
        try {
            Thread.sleep(pagingInterval);
        } catch (InterruptedException e) {
        }
        String res = result;
        result = null;
        return res;
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
