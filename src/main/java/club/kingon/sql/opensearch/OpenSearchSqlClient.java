package club.kingon.sql.opensearch;

/**
 * @author dragons
 * @date 2021/8/12 16:38
 */
public interface OpenSearchSqlClient {

    OpenSearchQueryIterator query(String sql);

    void close();
}
