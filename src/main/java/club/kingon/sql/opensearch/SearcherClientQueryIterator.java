package club.kingon.sql.opensearch;


import com.aliyun.opensearch.sdk.dependencies.org.json.JSONObject;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;

import java.util.Iterator;

/**
 * 搜索链
 * @author dragons
 * @date 2020-12-23 17:43
 */
public interface SearcherClientQueryIterator extends Iterator<SearchResult> {
    boolean hasNextOne();

    JSONObject nextOne();
}
