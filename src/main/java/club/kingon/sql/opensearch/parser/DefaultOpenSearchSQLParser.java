package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchManager;
import club.kingon.sql.opensearch.OpenSearchSqlClient;
import club.kingon.sql.opensearch.parser.entry.OpenSearchEntry;

/**
 * @author dragons
 * @date 2021/8/13 14:15
 */
public class DefaultOpenSearchSQLParser implements OpenSearchSQLParser {

    private OpenSearchManager manager;

    private OpenSearchSqlClient client;

    public DefaultOpenSearchSQLParser(OpenSearchManager manager, OpenSearchSqlClient client) {
        this.manager = manager;
        this.client = client;
    }

    @Override
    public <T extends OpenSearchEntry> T parse(String sql) {
        OpenSearchSQLType type = parseType(sql);
        if (type == OpenSearchSQLType.QUERY) {
            return (new DefaultOpenSearchQuerySQLParser(sql, manager)).parse(sql);
        } else if (type == OpenSearchSQLType.INSERT || type == OpenSearchSQLType.REPLACE){
            return (new DefaultOpenSearchInsertSQLParser(sql, manager)).parse(sql);
        } else if (type == OpenSearchSQLType.UPDATE) {
            return (new DefaultOpenSearchUpdateSQLParser(sql, manager, client)).parse(sql);
        } else if (type == OpenSearchSQLType.DELETE) {
            return (new DefaultOpenSearchDeleteSQLParser(sql, manager, client)).parse(sql);
        }
        throw new UnsupportedOperationException("unsupported sql operator. sql: " + sql);
    }

    private static OpenSearchSQLType parseType(String sql) {
        if (sql == null || sql.isEmpty()) {
            throw new SQLParserException("sql must be not empty.");
        }
        sql = sql.trim();
        OpenSearchSQLType[] descriptionTypes = OpenSearchSQLType.values();
        for (OpenSearchSQLType descriptionType : descriptionTypes) {
            if (sql.startsWith(descriptionType.getStartPrefix()) || sql.startsWith(descriptionType.getStartPrefix().toUpperCase())) {
                return descriptionType;
            }
        }
        throw new SQLParserException("cannot find sql type. please check your sql and then retry.");
    }
}
