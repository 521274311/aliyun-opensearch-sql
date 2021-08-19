package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchManager;

/**
 * @author dragons
 * @date 2021/8/19 13:43
 */
public class DefaultOpenSearchInsertSQLParser extends AbstractOpenSearchSQLParser{


    public DefaultOpenSearchInsertSQLParser(String sql, OpenSearchManager manager) {
        super(sql, manager);
    }

    @Override
    public <T extends OpenSearchEntry> T parse(String sql) {
        throw new UnsupportedOperationException("unsupported operation in current version");
    }
}
