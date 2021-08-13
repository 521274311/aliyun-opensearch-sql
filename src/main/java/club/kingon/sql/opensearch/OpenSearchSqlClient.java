package club.kingon.sql.opensearch;

import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;

import java.util.Iterator;
import java.util.List;

/**
 * @author dragons
 * @date 2021/8/12 16:38
 */
public interface OpenSearchSqlClient {

    Iterator<SearchResult> query(String sql);

    void close();
}
