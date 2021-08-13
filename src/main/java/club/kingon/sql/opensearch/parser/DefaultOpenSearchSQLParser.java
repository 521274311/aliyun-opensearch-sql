package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchManager;

/**
 * @author dragons
 * @date 2021/8/13 14:15
 */
public class DefaultOpenSearchSQLParser implements OpenSearchSQLParser {

    private OpenSearchManager manager;

    public DefaultOpenSearchSQLParser(OpenSearchManager manager) {
        this.manager = manager;
    }

    @Override
    public <T extends OpenSearchEntry> T parse(String sql) {
        OpenSearchSQLType type = parseType(sql);
        if (type == OpenSearchSQLType.QUERY) {
            return (new DefaultOpenSearchQuerySQLParser(sql, manager)).parse(sql);
        }
        throw new SqlParserException("unsupported parser type.");
    }

    private static OpenSearchSQLType parseType(String sql) {
        if (sql == null || sql.isEmpty()) {
            throw new SqlParserException("sql must be not empty.");
        }
        sql = sql.trim();
        OpenSearchSQLType[] descriptionTypes = OpenSearchSQLType.values();
        for (OpenSearchSQLType descriptionType : descriptionTypes) {
            if (sql.startsWith(descriptionType.getStartPrefix()) || sql.startsWith(descriptionType.getStartPrefix().toUpperCase())) {
                return descriptionType;
            }
        }
        throw new SqlParserException("cannot find sql type. please check your sql and then retry.");
    }
}
