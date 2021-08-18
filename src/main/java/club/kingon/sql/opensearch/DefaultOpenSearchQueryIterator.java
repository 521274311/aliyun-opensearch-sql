package club.kingon.sql.opensearch;

import club.kingon.sql.opensearch.parser.OpenSearchQueryEntry;
import club.kingon.sql.opensearch.util.Constants;
import club.kingon.sql.opensearch.util.OpenSearchBuilderUtil;
import club.kingon.sql.opensearch.util.ResponseConstants;
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

    private long pagingInterval = 200L;

    private long retryTimeInterval = 200L;

    private SearchResult result;

    private boolean alreadyExplain = false;
    private JSONArray items = null;

    public DefaultOpenSearchQueryIterator(SearcherClient client, OpenSearchQueryEntry data) {
        this.searcherClient = client;
        this.data = data;
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
                JSONObject resultJson = new JSONObject(searchResult.getResult());
                String status = resultJson.getString(ResponseConstants.STATUS);
                if (data.getQueryMode() == SearchQueryModeEnum.SCROLL) {
                    String scrollId = resultJson.getJSONObject(ResponseConstants.RESULT).getString(ResponseConstants.SCROLLID);
                    if (data.getDeepPaging().getScrollId() == null) {
                        data.getDeepPaging().setScrollId(scrollId);
                        searchResult = searcherClient.execute(searchParams);
                        resultJson = new JSONObject(searchResult.getResult());
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
                    if (!resultJson.has(ResponseConstants.RESULT)) {
                        if (log.isDebugEnabled()) {
                            log.debug("status is ok, but hasnot result param. info: {}", searchResult.getResult());
                        }
                    }
                    JSONArray items = resultJson.getJSONObject(ResponseConstants.RESULT).getJSONArray(ResponseConstants.RESULT_ITEMS);
                    if (items.length() > 0) {
                        result = searchResult;
                        if (data.getQueryMode() == SearchQueryModeEnum.HIT) {
                            data.setOffset(data.getOffset() + num);
                        }
                        data.setCount(data.getCount() - num);
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
        return data == null ? "" : OpenSearchBuilderUtil.builder(data).toString();
    }
}
