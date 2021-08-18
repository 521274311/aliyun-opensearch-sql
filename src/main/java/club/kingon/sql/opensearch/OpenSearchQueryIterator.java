package club.kingon.sql.opensearch;


import com.aliyun.opensearch.sdk.dependencies.org.json.JSONObject;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;

import java.util.Iterator;

/**
 * 搜索链
 * @author dragons
 * @date 2020-12-23 17:43
 */
public interface OpenSearchQueryIterator extends Iterator<SearchResult>, Expression<String> {
    boolean hasNextOne();

    JSONObject nextOne();
}
