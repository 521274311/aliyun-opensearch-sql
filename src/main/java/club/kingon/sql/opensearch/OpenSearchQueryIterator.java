package club.kingon.sql.opensearch;


import club.kingon.sql.opensearch.entry.OpenSearchQueryResult;
import club.kingon.sql.opensearch.entry.QueryObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;

import java.util.Iterator;

/**
 * 搜索链
 * @author dragons
 * @date 2020-12-23 17:43
 */
public interface OpenSearchQueryIterator extends Iterator<SearchResult>, Expression<String> {

    boolean hasSuccessfulNext();

    <T extends QueryObject> OpenSearchQueryResult<T> next(TypeReference<OpenSearchQueryResult<T>> clazz);
}
