package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchDqlException;
import club.kingon.sql.opensearch.OpenSearchManager;
import club.kingon.sql.opensearch.OpenSearchSqlClient;
import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.api.entry.Field;
import club.kingon.sql.opensearch.api.entry.Table;
import club.kingon.sql.opensearch.parser.entry.OpenSearchDataOperationEntry;
import club.kingon.sql.opensearch.parser.entry.OpenSearchEntry;
import club.kingon.sql.opensearch.parser.util.OpenSearchSqlConverter;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dragons
 * @date 2021/8/19 17:14
 */
public class DefaultOpenSearchDeleteSQLParser extends AbstractOpenSearchSQLParser{

    private final OpenSearchSqlClient client;

    private final static Logger log = LoggerFactory.getLogger(DefaultOpenSearchDeleteSQLParser.class);

    public DefaultOpenSearchDeleteSQLParser(String sql, OpenSearchManager manager, OpenSearchSqlClient client) {
        super(sql, manager);
        this.client = client;
    }

    @Override
    public <T extends OpenSearchEntry> T parse(String sql) {
        MySqlDeleteStatement statement = (MySqlDeleteStatement) this.statement;
        List<Tuple2<String, String>> appNameAndTables = getAppNameAndTables();
        if (appNameAndTables.size() == 0) throw new SQLParserException("update table must be not empty.");

        Table table = manager.getTable(statement.getTableName().getSimpleName());
        if (table == null) throw new SQLParserException("not fond table: " + statement.getTableName().getSimpleName() + ".");
        // 查找主键字段
        Field primary = table.getFields().values().stream().filter(Field::getPrimaryKey)
            .findFirst().orElse(null);
        if (primary == null) throw new OpenSearchDqlException("opensearch table should have less one primary column");
        if (statement.getWhere() == null) throw new OpenSearchDqlException("update must have less one condition.");
        OpenSearchDataOperationEntry entry = new OpenSearchDataOperationEntry();
        entry.setAppName(appNameAndTables.get(0).t1);
        entry.setTableName(appNameAndTables.get(0).t2);
        List<Map<String, Object>> data = new ArrayList<>();
        entry.setData(data);

        // 检查update条件是否为仅有主键，若仅有主键则直接对该主键数据update
        if (statement.getWhere() instanceof SQLBinaryOpExpr
            && ((SQLBinaryOpExpr) statement.getWhere()).getLeft() instanceof SQLIdentifierExpr
            && primary.getName().equals(((SQLIdentifierExpr) ((SQLBinaryOpExpr) statement.getWhere()).getLeft()).getName())
            && ((SQLBinaryOpExpr) statement.getWhere()).getRight() instanceof SQLValuableExpr) {
            Map<String, Object> map = new HashMap<>();
            map.put(primary.getName(), ((SQLValuableExpr) ((SQLBinaryOpExpr) statement.getWhere()).getRight()).getValue());
            data.add(map);
        }
        else if (statement.getWhere() instanceof SQLInListExpr
            && ((SQLInListExpr)statement.getWhere()).getExpr() instanceof SQLIdentifierExpr
            && primary.getName().equals(((SQLIdentifierExpr) ((SQLInListExpr) statement.getWhere()).getExpr()).getName())) {
            ((SQLInListExpr) statement.getWhere()).getTargetList().forEach(expr -> {
                if (expr instanceof SQLValuableExpr) {
                    Map<String, Object> map = new HashMap<>();
                    map.put(primary.getName(), ((SQLValuableExpr) expr).getValue());
                    data.add(map);
                } else {
                    log.warn("only support value type on current version, ignore expr: " + expr.toString());
                }
            });
        }
        // 否则通过查询出所有符合条件结果进行update
        else {
            throw new UnsupportedOperationException("unsupported update condition non primary column on current version");
        }
        return (T) entry;
    }

    private List<Tuple2<String, String>> getAppNameAndTables() {
        List<String> from = OpenSearchSqlConverter.explainFrom(visitor);
        return from.stream().limit(1).map(name -> {
            String[] appNameAndTable = name.split(",");
            if (appNameAndTable.length == 1) {
                return Tuple2.of(manager.getAppName().getName(), appNameAndTable[0]);
            }
            return Tuple2.of(appNameAndTable[0], appNameAndTable[1]);
        }).collect(Collectors.toList());
    }
}
