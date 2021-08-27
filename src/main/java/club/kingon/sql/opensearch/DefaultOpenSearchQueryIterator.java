package club.kingon.sql.opensearch;

import club.kingon.sql.opensearch.entry.OpenSearchQueryResult;
import club.kingon.sql.opensearch.entry.QueryObject;
import club.kingon.sql.opensearch.parser.entry.OpenSearchQueryEntry;
import club.kingon.sql.opensearch.util.OpenSearchBuilderUtil;
import club.kingon.sql.opensearch.util.ResponseConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.opensearch.SearcherClient;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchClientException;
import com.aliyun.opensearch.sdk.generated.commons.OpenSearchException;
import com.aliyun.opensearch.sdk.generated.search.*;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 完成一次查询迭代
 * 默认查询迭代器实现
 * 非线程安全
 * @see OpenSearchDqlException 所有方法均可能出现OpenSearchDqlException
 * @author dragons
 * @date 2020/12/23 18:29
 */
public class DefaultOpenSearchQueryIterator extends AbstractOpenSearchQueryIterator
        implements OpenSearchQueryIterator {

    private final static Logger log = LoggerFactory.getLogger(DefaultOpenSearchQueryIterator.class);

    private final SearcherClient searcherClient;

    private final OpenSearchQueryEntry data;

    private int retry = 0;

    private long pagingInterval = 10L;

    private long retryTimeInterval = 200L;

    private SearchResult result;

    public DefaultOpenSearchQueryIterator(SearcherClient client, OpenSearchQueryEntry data) {
        this.searcherClient = client;
        this.data = data;
    }

    @Override
    public boolean hasNext() {
        if (result != null) {
            return true;
        }
        if (data.getCount() == 0) {
            return false;
        }
        int num = Math.min(data.getCount(), data.getBatch());
        try {
            SearchParams searchParams = OpenSearchBuilderUtil.builder(data);
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
            if (data.getQueryMode() == SearchQueryModeEnum.SCROLL) {
                JSONObject resultJson = JSON.parseObject(searchResult.getResult());
                String scrollId = resultJson.getJSONObject(ResponseConstants.RESULT).getString(ResponseConstants.SCROLLID);
                if (data.getDeepPaging().getScrollId() == null) {
                    data.getDeepPaging().setScrollId(scrollId);
                    searchResult = searcherClient.execute(searchParams);
                    resultJson = JSON.parseObject(searchResult.getResult());
                    String status = resultJson.getString(ResponseConstants.STATUS);
                    if (ResponseConstants.STATUS_OK.equals(status)) {
                        scrollId = resultJson.getJSONObject(ResponseConstants.RESULT).getString(ResponseConstants.SCROLLID);
                    } else {
                        data.getDeepPaging().setScrollId(null);
                        if (log.isErrorEnabled()) {
                            log.error("scroll mode request fail, info: {}", searchResult.getResult());
                        }
                    }
                }
                data.getDeepPaging().setScrollId(scrollId);
            } else if (data.getQueryMode() == SearchQueryModeEnum.HIT) {
                data.setOffset(data.getOffset() + num);
            }
            data.setCount(data.getCount() - num);
            result = searchResult;
            return true;
        } catch (OpenSearchException | OpenSearchClientException e) {
            if (log.isErrorEnabled()) {
                log.error("OpenSearch exception", e);
            }
            if (retry < 0) {
                throw new OpenSearchDqlException(e);
            }
        }
        return false;
    }

    @Override
    public boolean hasSuccessfulNext() {
        int retry = this.retry;
        if (result != null) {
            return true;
        }
        if (data.getCount() == 0) {
            return false;
        }
        int num = Math.min(data.getCount(), data.getBatch());
        while (retry-- >= 0) {
            try {
                SearchParams searchParams = OpenSearchBuilderUtil.builder(data);
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
                JSONObject resultJson = JSON.parseObject(searchResult.getResult());
                String status = resultJson.getString(ResponseConstants.STATUS);
                if (data.getQueryMode() == SearchQueryModeEnum.SCROLL) {
                    String scrollId = resultJson.getJSONObject(ResponseConstants.RESULT).getString(ResponseConstants.SCROLLID);
                    if (data.getDeepPaging().getScrollId() == null) {
                        data.getDeepPaging().setScrollId(scrollId);
                        searchResult = searcherClient.execute(searchParams);
                        resultJson = JSON.parseObject(searchResult.getResult());
                        status = resultJson.getString(ResponseConstants.STATUS);
                        if (ResponseConstants.STATUS_OK.equals(status)) {
                            scrollId = resultJson.getJSONObject(ResponseConstants.RESULT).getString(ResponseConstants.SCROLLID);
                        } else {
                            data.getDeepPaging().setScrollId(null);
                            if (log.isErrorEnabled()) {
                                log.error("scroll mode request fail, info: {}", searchResult.getResult());
                            }
                        }
                    }
                    data.getDeepPaging().setScrollId(scrollId);
                }
                if (ResponseConstants.STATUS_OK.equals(status)) {
                    if (!resultJson.containsKey(ResponseConstants.RESULT)) {
                        if (log.isDebugEnabled()) {
                            log.debug("status is ok, but hasnot result param. info: {}", searchResult.getResult());
                        }
                    }
                    JSONArray items = resultJson.getJSONObject(ResponseConstants.RESULT).getJSONArray(ResponseConstants.RESULT_ITEMS);
                    if (items.size() > 0) {
                        result = searchResult;
                        if (data.getQueryMode() == SearchQueryModeEnum.HIT) {
                            data.setOffset(data.getOffset() + num);
                        }
                        data.setCount(data.getCount() - num);
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

    private void waitPagingInterval() {
        try {
            Thread.sleep(pagingInterval);
        } catch (InterruptedException e) {
        }
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public long getPagingInterval() {
        return pagingInterval;
    }

    public void setPagingInterval(long pagingInterval) {
        this.pagingInterval = pagingInterval;
    }

    public long getRetryTimeInterval() {
        return retryTimeInterval;
    }

    public void setRetryTimeInterval(long retryTimeInterval) {
        this.retryTimeInterval = retryTimeInterval;
    }

    @Override
    public String express() {
        return data == null ? "" : data.getCount() == 0 ? "finish" : OpenSearchBuilderUtil.builder(data).toString();
    }

    /**
     *
     */
    @Override
    public <T extends QueryObject> OpenSearchQueryResult<T> next(TypeReference<OpenSearchQueryResult<T>> clazz) {
        waitPagingInterval();
        if (this.result == null) return null;
        OpenSearchQueryResult<T> res = JSON.parseObject(this.result.getResult(), clazz);
        result = null;
        return res;
    }
}
