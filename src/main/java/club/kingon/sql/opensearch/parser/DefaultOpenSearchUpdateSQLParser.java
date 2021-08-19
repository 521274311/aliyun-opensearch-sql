package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchManager;

/**
 * @author dragons
 * @date 2021/8/19 13:59
 */
public class DefaultOpenSearchUpdateSQLParser extends AbstractOpenSearchSQLParser {

    public DefaultOpenSearchUpdateSQLParser(String sql, OpenSearchManager manager) {
        super(sql, manager);
    }

    @Override
    public <T extends OpenSearchEntry> T parse(String sql) {
        throw new UnsupportedOperationException("unsupported operation in current version");
    }
}
