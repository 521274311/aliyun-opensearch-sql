package club.kingon.sql.opensearch;

import com.aliyun.opensearch.sdk.generated.commons.OpenSearchResult;
import com.aliyun.opensearch.sdk.generated.search.Distinct;

import java.util.Set;

/**
 * @author dragons
 * @date 2021/8/12 16:38
 */
public interface OpenSearchSqlClient {

    default OpenSearchQueryIterator query(String sql) {
        return query(sql, null);
    }

    OpenSearchQueryIterator query(String sql, Set<Distinct> distincts);

    OpenSearchResult insert(String sql);

    OpenSearchResult update(String sql);

    OpenSearchResult delete(String sql);

    void close();
}
