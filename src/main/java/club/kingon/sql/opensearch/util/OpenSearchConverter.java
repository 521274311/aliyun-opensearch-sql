package club.kingon.sql.opensearch.util;

import club.kingon.sql.opensearch.OpenSearchDqlException;
import club.kingon.sql.opensearch.Tuple2;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.aliyun.opensearch.sdk.generated.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <p>OpenSearch Sql转换器</p>
 * 使用语法：
 *  select
 * 	    [Distinct打散]
 * 	    [aggregate(count,sum,max,min)(聚合)]
 * 	    [field(字段)..]
 *  from [appName1],[appName2],..
 *  where
 *      ([index(索引)[=|like]character] [..OR|AND])
 * 	    [..AND|OR]
 *      ([index(索引)[=|>|>=|<|<=]number] [..OR|AND])
 *  group by [field]
 *  order by [field] [asc|desc]
 *  limit min,count
 * @author dragons
 * @date 2020/12/18 16:13
 */
public class OpenSearchConverter {

    private final static Logger log = LoggerFactory.getLogger(OpenSearchConverter.class);

    public static List<String> explainFrom(MySqlSchemaStatVisitor visitor) {
        Set<TableStat.Name> names = visitor.getTables().keySet();
        List<String> appNames = new ArrayList<>(names.size());
        names.forEach(n -> appNames.add(n.getName()));
        return appNames;
    }

    /**
     * https://help.aliyun.com/document_detail/180150.html
     * @param limit sql limit
     * @return Tuple2<start, hit>
     */
    public static Tuple2<Integer, Integer> explainStartAndHit(SQLLimit limit) {
        Integer offset = null, count = null;
        if (limit != null) {
            if (limit.getOffset() != null) {
                offset = (int) ((SQLIntegerExpr) limit.getOffset()).getNumber();
            }
            if (limit.getRowCount() != null) {
                count = (int) ((SQLIntegerExpr) limit.getRowCount()).getNumber();
                if (offset == null) {
                    offset = 0;
                }
            }
        }
        return Tuple2.of(offset, count);
    }

    public static List<String> explainFetchField(MySqlSelectQueryBlock block) {
        List<SQLSelectItem> sqlSelectItemList = block.getSelectList();
        if (sqlSelectItemList == null || sqlSelectItemList.size() <= 0
                || sqlSelectItemList.get(0).getExpr() instanceof SQLAllColumnExpr) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (SQLSelectItem x : sqlSelectItemList) {
            if (x.getExpr() instanceof SQLIdentifierExpr) {
                result.add(((SQLIdentifierExpr) x.getExpr()).getLowerName());
            } else if (x.getExpr() instanceof SQLAggregateExpr) {
                result.add(((SQLAggregateExpr) x.getExpr()).getMethodName().toLowerCase());
            } else if (x.getExpr() instanceof SQLAllColumnExpr) {
                result.clear();
                break;
            }
        }
        return result;
    }

    /**
     * todo 别名提取
     * @return Map<index_name, alias_name>
     */
    public static Map<String, String> explainFetchFieldsAndAlias(MySqlSelectQueryBlock block) {
        List<SQLSelectItem> sqlSelectItemList = block.getSelectList();
        if (sqlSelectItemList == null || sqlSelectItemList.size() <= 0
                || sqlSelectItemList.get(0).getExpr() instanceof SQLAllColumnExpr) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        sqlSelectItemList.forEach(x -> {
            String fieldName = x.getExpr() instanceof SQLIdentifierExpr ? ((SQLIdentifierExpr)x.getExpr()).getLowerName()
                    : ((SQLAggregateExpr) x.getExpr()).getMethodName().toLowerCase();
            result.put(fieldName,
                    x.getAlias() != null && !Constants.EMPTY_STRING.equals(x.getAlias()) ? x.getAlias()
                            : fieldName);
        });
        return result;
    }

    public static Tuple2<String, String> explainQueryAndFilter(SQLBinaryOpExpr expr) {
        Tuple2<String, String> queryAndFilter = getQueryAndFilter(expr);
        if ("".equals(queryAndFilter.t1)) {
            queryAndFilter.t1 = null;
        }
        if ("".equals(queryAndFilter.t2)) {
            queryAndFilter.t2 = null;
        }
        return queryAndFilter;
    }
    /**
     * https://help.aliyun.com/document_detail/180014.html
     * https://help.aliyun.com/document_detail/180028.html
     * https://help.aliyun.com/document_detail/121966.html
     * Range 提取方式使用between and函数提取
     * 下钻一级递归提取query和filter
     * @return Tuple2<query, filter>
     */
    private static Tuple2<String, String> getQueryAndFilter(SQLBinaryOpExpr expr) {
        SQLExpr leftChildSqlExpr = null, rightChildSqlExpr = null;
        if (expr == null || ((leftChildSqlExpr = expr.getLeft()) == null & (rightChildSqlExpr = expr.getRight()) == null)) {
            return Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING);
        }
        boolean leftOp, rightOp;
        String opName = getQueryAndFilterBinaryOperatorName(expr.getOperator());
        if (!(leftOp = isBinary(leftChildSqlExpr)) & !(rightOp = isBinary(rightChildSqlExpr))) {
            Object left = resolveQueryAndFilterSQLExpr(leftChildSqlExpr, false);
            Object right = resolveQueryAndFilterSQLExpr(rightChildSqlExpr, false);
            // 使用like指定使用query
            if (Constants.LIKE.equalsIgnoreCase(opName)) {
                String value = right.toString();
                boolean hasChinese = PatternUtil.hasChinese(value);
                if (value != null && !value.isEmpty()) {
                    if (value.charAt(0) != Constants.PERCENT_SIGN_CHARACTER) {
                        if (!hasChinese) {
                            value = Constants.HEAD_TERMINATOR + value;
                        }
                    } else {
                        value = value.substring(1);
                    }
                    if (value.charAt(value.length() - 1) != Constants.PERCENT_SIGN_CHARACTER) {
                        if (!hasChinese) {
                            value = value + Constants.TAIL_TERMINATOR;
                        }
                    } else {
                        value = value.substring(0, value.length() - 1);
                    }
                    return Tuple2.of(left
                            + Constants.COLON_SINGLE_QUOTES + value + Constants.SINGLE_QUOTES_MARK,
                        Constants.EMPTY_STRING);
                }
                return Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING);
            }
            // 其他表达式统一使用filter
            else {
                String value = right.toString();
                if (rightChildSqlExpr instanceof SQLTextLiteralExpr) {
                    value = Constants.DOUBLE_QUOTES_MARK + value + Constants.DOUBLE_QUOTES_MARK;
                }
                return Tuple2.of(Constants.EMPTY_STRING, left + opName + value);
            }
        }
        else if (leftOp && rightOp){
            Tuple2<String, String> left = (Tuple2<String, String>) resolveQueryAndFilterSQLExpr(leftChildSqlExpr, true);
            Tuple2<String, String> right = (Tuple2<String, String>) resolveQueryAndFilterSQLExpr(rightChildSqlExpr, true);
            return mergeQueryAndFilter(left, right, opName);
        }
        // 复合表达式仅支持filter
        else if (leftOp) {
            Tuple2<String, String> left = (Tuple2<String, String>) resolveQueryAndFilterSQLExpr(leftChildSqlExpr, true);
            String right = getFilterValue(rightChildSqlExpr);
            if (!Constants.EMPTY_STRING.equals(left.t2)) {
                left.t2 = left.t2 + opName + right;
            }
            return left;
        } else {
            Object left = resolveQueryAndFilterSQLExpr(leftChildSqlExpr, false);
            Tuple2<String, String> right = (Tuple2<String, String>) resolveQueryAndFilterSQLExpr(rightChildSqlExpr, true);
            if (!Constants.EMPTY_STRING.equals(right.t2)) {
                right.t2 = left +opName + right.t2;
            }
            return right;
        }
//        return Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING);
    }

    private static Object resolveQueryAndFilterSQLExpr(SQLExpr expr) {
        return resolveQueryAndFilterSQLExpr(expr, true);
    }

    /**
     * https://help.aliyun.com/document_detail/204346.html
     * @param op 是否是表达式类型expr，为false表示表达式一部分的expr，例如SQLMethodInvokeExpr类型获取的结果只是一个值域
     * @return
     */
    private static Object resolveQueryAndFilterSQLExpr(SQLExpr expr, boolean op) {
        // binary
        if (expr instanceof SQLInListExpr) return getQueryAndFilter((SQLInListExpr)expr);
        if (expr instanceof SQLBinaryOpExpr) return getQueryAndFilter((SQLBinaryOpExpr) expr);
        if (expr instanceof SQLBetweenExpr) return getQueryAndFilter((SQLBetweenExpr) expr);

        // maybe binary or not binary, current version is not binary
        if (expr instanceof SQLMethodInvokeExpr) return getQueryAndFilter((SQLMethodInvokeExpr) expr, false);
        // not binary
        if (expr instanceof SQLDefaultExpr) return getQueryAndFilter((SQLDefaultExpr) expr);
        if (expr instanceof SQLIdentifierExpr) return getQueryAndFilter((SQLIdentifierExpr) expr);
        if (expr instanceof SQLNumericLiteralExpr) return getQueryAndFilter((SQLNumericLiteralExpr) expr);
        if (expr instanceof SQLTextLiteralExpr) return getQueryAndFilter((SQLTextLiteralExpr) expr);
        return op ? Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING) : "";
    }

    private static boolean isBinary(SQLExpr expr) {
        if (expr instanceof SQLInListExpr) return true;
        if (expr instanceof SQLBinaryOpExpr)  return true;
        if (expr instanceof SQLBetweenExpr) return true;
        return false;
    }

    private static String getQueryAndFilter(SQLDefaultExpr expr) {
        return "default";
    }

    private static String getQueryAndFilter(SQLIdentifierExpr expr) {
        return expr.getLowerName();
    }

    private static Object getQueryAndFilter(SQLNumericLiteralExpr expr) {
        return expr.getNumber();
    }

    private static String getQueryAndFilter(SQLTextLiteralExpr expr) {
        return expr.getText();
    }

    /**
     * https://help.aliyun.com/document_detail/204346.html
     */
    private static String getQueryAndFilter(SQLMethodInvokeExpr expr, boolean op) {
        // OpenSearch 函数不支持单引号, 此处需转换
        return expr.toString().replace("'", "\"");
    }

    private static Tuple2<String, String> getQueryAndFilter(SQLBetweenExpr expr) {
        SQLExpr textExpr = expr.testExpr;
        return Tuple2.of(Constants.EMPTY_STRING, ((SQLIdentifierExpr) textExpr).getLowerName() + ":["
            + ((SQLValuableExpr) expr.beginExpr).getValue() + "," + ((SQLValuableExpr) expr.endExpr).getValue() + "]");
    }

    private static Tuple2<String, String> getQueryAndFilter(SQLInListExpr expr) {
        String exp = expr.isNot() ? "notin" : "in";
        Object name = expr.getExpr() instanceof SQLIdentifierExpr ? ((SQLIdentifierExpr)expr.getExpr()).getLowerName() : resolveQueryAndFilterSQLExpr(expr.getExpr());
        StringBuilder builder = new StringBuilder(exp + "(" + name + ", \"");
        for (int i = 0; i < expr.getTargetList().size(); i++) {
            if (i > 0) builder.append("|");
            if (expr.getTargetList().get(i) instanceof SQLValuableExpr) {
                builder.append(((SQLValuableExpr) expr.getTargetList().get(i)).getValue());
            }
        }
        builder.append("\")");
        return Tuple2.of(Constants.EMPTY_STRING, builder.toString());
    }

    private static String getFilterValue(SQLExpr expr) {
        if (expr instanceof SQLTextLiteralExpr) {
            return Constants.DOUBLE_QUOTES_MARK + ((SQLTextLiteralExpr) expr).getText() + Constants.DOUBLE_QUOTES_MARK;
        } else if (expr instanceof SQLNumericLiteralExpr) {
            return ((SQLNumericLiteralExpr) expr).getNumber().toString();
        }
        return resolveQueryAndFilterSQLExpr(expr, false).toString();
    }

    private static String getQueryAndFilterBinaryOperatorName(SQLBinaryOperator op) {
        if (Constants.LESS_AND_GREATER.equals(op.name)) {
            return Constants.NE_EQUAL_SIGN;
        }
        return op.name;
    }

    private static Tuple2<String, String> mergeQueryAndFilter(Tuple2<String, String> leftTp, Tuple2<String, String> rightTp, String operatorName) {
        StringBuilder query = new StringBuilder(), filter = new StringBuilder();
        if (!Constants.EMPTY_STRING.equals(leftTp.t1)) {
            query.append(leftTp.t1);
        }
        if (!Constants.EMPTY_STRING.equals(leftTp.t2)) {
            filter.append(leftTp.t2);
        }
        if (!Constants.EMPTY_STRING.equals(rightTp.t1)) {
            if (query.length() > 0) {
                query.insert(0, Constants.SPACE_LEFT_SMALL_BRACKET).append(Constants.SPACE_STRING).append(operatorName).append(Constants.SPACE_STRING)
                    .append(rightTp.t1).append(Constants.RIGHT_SMALL_BRACKET_SPACE);
            } else {
                query.append(rightTp.t1);
            }
        }
        if (!Constants.EMPTY_STRING.equals(rightTp.t2)) {
            if (filter.length() > 0) {
                filter.insert(0, Constants.SPACE_LEFT_SMALL_BRACKET).append(Constants.SPACE_STRING).append(operatorName).append(Constants.SPACE_STRING)
                    .append(rightTp.t2).append(Constants.RIGHT_SMALL_BRACKET_SPACE);
            } else {
                filter.append(rightTp.t2);
            }
        }
        return Tuple2.of(query.toString(), filter.toString());
    }
    /**
     * https://help.aliyun.com/document_detail/180073.html
     */
    public static Set<Distinct> explainDistinct(MySqlSelectQueryBlock block) {
        // 检查distinct状态
        if (block.getDistionOption() != 2) return null;
        Set<Distinct> distinctSet = new HashSet<>();
        for (SQLSelectItem item : block.getSelectList()) {
            SQLExpr expr = item.getExpr();
            if (expr instanceof SQLAggregateExpr) {
                SQLAggregateOption option = ((SQLAggregateExpr) expr).getOption();
                SQLExpr aggExpr;
                if (option != null && Constants.DISTINCT.equalsIgnoreCase(option.name())) {
                    List<SQLExpr> arguments = ((SQLAggregateExpr) expr).getArguments();
                    if (arguments.size() > 1) {
                        throw new OpenSearchDqlException("OpenSearch distinct is used, the select field total only less than one");
                    }
                    else if (arguments.size() == 1) {
                        if ((aggExpr = arguments.get(0)) instanceof SQLIdentifierExpr) {
                            distinctSet.add(new Distinct(((SQLIdentifierExpr) aggExpr).getLowerName()) {{
                                setReserved(false);
                                setUpdateTotalHit(true);
                            }});
                        } else {
                            log.warn("distinct: exists illegal SQLExpr type => {}, and this will be ignored. only SQLIdentifierExpr type is allowed", expr.getClass().getSimpleName());
                        }
                    } else {

                    }
                }
            } else if (expr instanceof SQLIdentifierExpr) {
                distinctSet.add(new Distinct(((SQLIdentifierExpr) expr).getLowerName()) {{
                    setReserved(false);
                    setUpdateTotalHit(true);
                }});
            } else {
                log.warn("distinct: exists illegal SQLExpr type => {}, and this will be ignored. only SQLIdentifierExpr type is allowed", expr.getClass().getSimpleName());
            }
        }
        return !distinctSet.isEmpty() ? distinctSet : null;
    }

    /**
     * https://help.aliyun.com/document_detail/180049.html
     */
    public static Set<Aggregate> explainAggregate(MySqlSelectQueryBlock block, MySqlSchemaStatVisitor visitor) {
        Set<TableStat.Column> groupByColumns = visitor.getGroupByColumns();
        Set<Aggregate> aggregateSet = new HashSet<>();
        List<SQLAggregateExpr> aggregateFunctions = visitor.getAggregateFunctions();
        List<String> effectGroupByColumnNames = new ArrayList<>();
        Aggregate aggregate = null;
        // field
        if (groupByColumns != null && !groupByColumns.isEmpty()) {
            aggregate = new Aggregate();
            for (TableStat.Column column : groupByColumns) {
                effectGroupByColumnNames.add(column.getName());
            }
        }
        StringBuilder aggFunBuilder = new StringBuilder();
        // group by
        if (aggregateFunctions != null && !aggregateFunctions.isEmpty()) {
            if (aggregate == null) {
                throw new OpenSearchDqlException("AggFun must have groupKey.");
            }
            String aggFunName;
            List<SQLExpr> arguments;
            for (SQLAggregateExpr aggregateExpr : aggregateFunctions) {
                aggFunName = aggregateExpr.getMethodName().toLowerCase();
                arguments = aggregateExpr.getArguments();
                if (!Constants.COUNT_FUNCTION.equals(aggFunName) && (arguments == null || arguments.size() != 1)) {
                    throw new OpenSearchDqlException("AggFun that is not count function must have a argument.");
                }
                Object argumentName = "";
                if (!Constants.COUNT_FUNCTION.equals(aggFunName)) {
                    SQLExpr argumentExpr = arguments.get(0);
                    if (argumentExpr instanceof SQLValuableExpr) {
                        argumentName = ((SQLValuableExpr) argumentExpr).getValue();
                    } else if (argumentExpr instanceof SQLIdentifierExpr) {
                        argumentName = ((SQLIdentifierExpr) argumentExpr).getLowerName();
                    } else {
                        throw new OpenSearchDqlException("AggFun's argument only support attribute field.");
                    }
                }
                switch (aggFunName) {
                    case Constants.COUNT_FUNCTION:
                    case Constants.MAX_FUNCTION:
                    case Constants.MIN_FUNCTION:
                    case Constants.SUM_FUNCTION:
                        aggFunBuilder.append(aggFunName).append(Constants.LEFT_SMALL_BRACKET).append(argumentName)
                                .append(Constants.RIGHT_SMALL_BRACKET).append("#"); break;
                    default:
                        throw new OpenSearchDqlException("AggFun only allow to use count,sum,max,min. Your aggFun: " + aggFunName);
                }
            }
        }
        if (!effectGroupByColumnNames.isEmpty() && aggFunBuilder.length() > 0) {
            for (String effectGroupByColumnName : effectGroupByColumnNames) {
                aggregateSet.add(new Aggregate() {{
                    setGroupKey(effectGroupByColumnName);
                    setAggFun(aggFunBuilder.substring(0, aggFunBuilder.length() - 1));
                }});
            }
        }
        return !aggregateSet.isEmpty() ? aggregateSet : null;
    }

    /**
     * https://help.aliyun.com/document_detail/180032.html
     */
    public static Sort explainSort(MySqlSelectQueryBlock block) {
        if (block.getOrderBy() == null) {
            return null;
        }
        List<SQLSelectOrderByItem> orderByItems = block.getOrderBy().getItems();
        List<SortField> sortFields = new ArrayList<>(orderByItems.size());
        orderByItems.forEach(item -> sortFields.add(new SortField(((SQLIdentifierExpr)item.getExpr()).getLowerName(),
                item.getType() == null || Constants.INCREASE.equalsIgnoreCase(item.getType().name) ? Order.INCREASE : Order.DECREASE)));
        return !sortFields.isEmpty() ? new Sort(sortFields) : null;
    }
}


