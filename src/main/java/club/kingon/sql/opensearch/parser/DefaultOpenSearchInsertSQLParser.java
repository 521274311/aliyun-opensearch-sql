package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchManager;
import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.parser.entry.OpenSearchEntry;
import club.kingon.sql.opensearch.parser.entry.OpenSearchDataOperationEntry;
import club.kingon.sql.opensearch.parser.util.OpenSearchSqlConverter;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dragons
 * @date 2021/8/19 13:43
 */
public class DefaultOpenSearchInsertSQLParser extends AbstractOpenSearchSQLParser{

    private final static Logger log = LoggerFactory.getLogger(DefaultOpenSearchInsertSQLParser.class);

    public DefaultOpenSearchInsertSQLParser(String sql, OpenSearchManager manager) {
        super(sql, manager);
    }

    @Override
    public <T extends OpenSearchEntry> T parse(String sql) {
        MySqlInsertStatement statement = (MySqlInsertStatement) this.statement;
        List<Tuple2<String, String>> appNameAndTables = getAppNameAndTables();
        if (appNameAndTables.size() == 0) throw new SQLParserException("insert table must be not empty.");

        OpenSearchDataOperationEntry entry = new OpenSearchDataOperationEntry();
        entry.setAppName(appNameAndTables.get(0).t1);
        entry.setTableName(appNameAndTables.get(0).t2);
        List<Map<String, Object>> data = new ArrayList<>();
        entry.setData(data);

        List<String> columns = statement.getColumns().stream().filter(c -> c instanceof SQLIdentifierExpr)
            .map(c -> ((SQLIdentifierExpr) c).getName()).collect(Collectors.toList());
        statement.getValuesList()
            .forEach(valuesClause -> {
                if (valuesClause.getValues().size() != columns.size()) {
                    log.warn("someone column data length mismatch compared to column names. please check your code and retry.");
                } else {
                    Map<String, Object> one = new HashMap<>();
                    for (int i = 0; i < valuesClause.getValues().size(); i++) {
                        one.put(columns.get(i), ((SQLValuableExpr) valuesClause.getValues().get(i)).getValue());
                    }
                    data.add(one);
                }
            });
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
