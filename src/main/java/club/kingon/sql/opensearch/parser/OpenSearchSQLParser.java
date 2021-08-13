package club.kingon.sql.opensearch.parser;

/**
 * @author dragons
 * @date 2021/8/13 14:14
 */
public interface OpenSearchSQLParser {

    <T extends OpenSearchEntry> T parse(String sql);
}
