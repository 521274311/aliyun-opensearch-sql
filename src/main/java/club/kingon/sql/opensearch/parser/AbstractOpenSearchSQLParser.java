package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchManager;
import club.kingon.sql.opensearch.util.Constants;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;

/**
 * @author dragons
 * @date 2021/8/13 14:45
 */
public abstract class AbstractOpenSearchSQLParser implements OpenSearchSQLParser {

    protected SQLStatement statement;

    protected MySqlSchemaStatVisitor visitor;

    protected OpenSearchManager manager;

    protected AbstractOpenSearchSQLParser(String sql, OpenSearchManager manager) {
        statement = SQLUtils.parseSingleStatement(sql, Constants.MYSQL_DB_TYPE);
        visitor = new MySqlSchemaStatVisitor();
        statement.accept(visitor);
        this.manager = manager;
    }
}
