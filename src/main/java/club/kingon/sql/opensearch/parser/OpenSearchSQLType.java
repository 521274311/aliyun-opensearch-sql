package club.kingon.sql.opensearch.parser;

/**
 * @author dragons
 * @date 2021/8/13 14:19
 */
public enum OpenSearchSQLType {
    INSERT("insert"),
    UPDATE("update"),
    REPLACE("replace"),
    QUERY("select"),
    DELETE("delete");

    private String startPrefix;

    OpenSearchSQLType(String startPrefix) {
        this.startPrefix = startPrefix;
    }

    public String getStartPrefix() {
        return startPrefix;
    }
}
