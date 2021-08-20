package club.kingon.sql.opensearch.parser.util;

import club.kingon.sql.opensearch.OpenSearchDqlException;
import club.kingon.sql.opensearch.OpenSearchManager;
import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.parser.SQLParserException;
import club.kingon.sql.opensearch.support.AbstractOpenSearchAppNameManager;
import club.kingon.sql.opensearch.support.util.OpenSearchCheckUtil;
import club.kingon.sql.opensearch.util.Constants;
import club.kingon.sql.opensearch.util.PatternUtil;
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
import java.util.stream.Collectors;

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
public class OpenSearchSqlConverter {

    private final static Logger log = LoggerFactory.getLogger(OpenSearchSqlConverter.class);

    public static List<String> explainFrom(MySqlSchemaStatVisitor visitor) {
        return explainFrom(visitor, null);
    }

    public static List<String> explainFrom(MySqlSchemaStatVisitor visitor, OpenSearchManager manager) {
        Set<TableStat.Name> names = visitor.getTables().keySet();
        List<String> appNames = new ArrayList<>(names.size());
        names.forEach(n -> appNames.add(n.getName()));
        return appNames;
    }

    public static List<Tuple2<String, String>> explainAppNameAndTable(MySqlSchemaStatVisitor visitor, OpenSearchManager manager) {
        List<String> from = OpenSearchSqlConverter.explainFrom(visitor, manager);
        return from.stream().limit(1).map(name -> {
            String[] appNameAndTable = name.split(",");
            if (appNameAndTable.length == 1) {
                if (manager instanceof AbstractOpenSearchAppNameManager && ((AbstractOpenSearchAppNameManager) manager).isEnableAppNameManagement()) {
                    return Tuple2.of(manager.getAppName().getName(), appNameAndTable[0]);
                } else {
                    throw new SQLParserException("disable appname management or appname management is starting. please whether enableManagement value is true or increasing waitMills. or maybe you can rewrite 'table' -> 'appname.table'.");
                }
            }
            return Tuple2.of(appNameAndTable[0], appNameAndTable[1]);
        }).collect(Collectors.toList());
    }

    /**
     * https://help.aliyun.com/document_detail/180150.html
     * @param limit sql limit
     * @return Tuple2<start, hit>
     */
    public static Tuple2<Integer, Integer> explainStartAndHit(SQLLimit limit) {
        return explainStartAndHit(limit, null);
    }

    public static Tuple2<Integer, Integer> explainStartAndHit(SQLLimit limit, OpenSearchManager manager) {
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
        return explainFetchField(block, null);
    }

    public static List<String> explainFetchField(MySqlSelectQueryBlock block, OpenSearchManager manager) {
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

    public static Tuple2<Tuple2<String, String>, Map<String, Object>> explainWhere(SQLExpr expr, OpenSearchManager manager) {
        Tuple2<Tuple2<String, String>, Map<String, Object>> queryAndFilter = (Tuple2<Tuple2<String, String>, Map<String, Object>>) resolveQueryAndFilterSQLExpr(expr, true, manager);
        if (Constants.EMPTY_STRING.equals(queryAndFilter.t1.t1)) {
            queryAndFilter.t1.t1 = null;
        }
        if (Constants.EMPTY_STRING.equals(queryAndFilter.t1.t2)) {
            queryAndFilter.t1.t2 = null;
        }
        return queryAndFilter;
    }

    public static Rank expainRank(Map<String, Object> mp) {
        Rank rank = new Rank();
        String firstRankName = (String) mp.get(Constants.DEFAULT_FIRST_RANK_NAME),
            secondRankName = (String) mp.get(Constants.DEFAULT_SECOND_RANK_NAME);
        Object reRankSize = mp.get(Constants.DEFAULT_RE_RANK_SIZE_NAME);
        if (firstRankName != null) {
            rank.setFirstRankName(firstRankName);
        }
        if (secondRankName != null) {
            rank.setSecondRankName(secondRankName);
        }
        if (reRankSize != null) {
            rank.setReRankSize(Integer.parseInt(String.valueOf(reRankSize)));
        }
        return rank;
    }
    /**
     * https://help.aliyun.com/document_detail/180014.html
     * https://help.aliyun.com/document_detail/180028.html
     * https://help.aliyun.com/document_detail/121966.html
     * Range 提取方式使用between and函数提取
     * 下钻一级递归提取query和filter
     * @return Tuple2<query, filter>
     */
    private static Tuple2<Tuple2<String, String>, Map<String, Object>> getQueryAndFilter(SQLBinaryOpExpr expr, OpenSearchManager manager) {
        SQLExpr leftChildSqlExpr = null, rightChildSqlExpr = null;
        if (expr == null || ((leftChildSqlExpr = expr.getLeft()) == null & (rightChildSqlExpr = expr.getRight()) == null)) {
            return Tuple2.of(getEmptyStringTuple2(), Collections.emptyMap());
        }
        boolean leftOp, rightOp;
        String opName = getQueryAndFilterBinaryOperatorName(expr.getOperator()), opNameLower = opName.toLowerCase();
        if (!(leftOp = isBinary(leftChildSqlExpr)) & !(rightOp = isBinary(rightChildSqlExpr))) {
            Object left = resolveQueryAndFilterSQLExpr(leftChildSqlExpr, false, manager);
            Object right = resolveQueryAndFilterSQLExpr(rightChildSqlExpr, false, manager);
            // 使用like指定使用query
            if (Constants.LIKE.equals(opNameLower) || Constants.NOT_LIKE.equals(opNameLower)) {
                String name = left.toString();
                String value = right.toString();
                boolean hasChinese = PatternUtil.hasChinese(value);
                if (value != null && !value.isEmpty()) {
                    // 管理器获取索引是否支持模糊查询,若不支持则直接删除value中前后百分号
                    if (manager != null && !OpenSearchCheckUtil.supportFuzzyQuery(manager.getSearchIndexFields(name))) {
                        if (value.charAt(0) == Constants.PERCENT_SIGN_CHARACTER) {
                            value = value.substring(1);
                        }
                        if (value.charAt(value.length() - 1) == Constants.PERCENT_SIGN_CHARACTER) {
                            value = value.substring(0, value.length() - 1);
                        }
                    }
                    else {
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
                    }

                    return Tuple2.of(Tuple2.of(name
                            + Constants.COLON_SINGLE_QUOTES + value + Constants.SINGLE_QUOTES_MARK,
                        Constants.EMPTY_STRING), Collections.emptyMap());
                }
                return Tuple2.of(Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING), Collections.emptyMap());
            }
            // 其他表达式统一使用filter
            else {
                String name = left.toString(), value = right.toString();
                if (isInnerParamName(name)) {
                    return resolveInnerParam(name, value);
                }
                // 添加管理器处理等号，针对属性字段不存在，索引字段存在属性转为query查询
                if (manager != null && !manager.existsFilterAttribute(name) && manager.existsSearchIndex(name) && Constants.EQUAL_SIGN.equals(opName)) {
                    return Tuple2.of(Tuple2.of(name + Constants.COLON_SINGLE_QUOTES + value + Constants.SINGLE_QUOTES_MARK, Constants.EMPTY_STRING), Collections.emptyMap());
                }
                if (rightChildSqlExpr instanceof SQLTextLiteralExpr) {
                    value = Constants.DOUBLE_QUOTES_MARK + value + Constants.DOUBLE_QUOTES_MARK;
                }
                return Tuple2.of(Tuple2.of(Constants.EMPTY_STRING, name + opName + value), Collections.emptyMap());
            }
        }
        else if (leftOp && rightOp){
            Tuple2<Tuple2<String, String>, Map<String, Object>> left = (Tuple2<Tuple2<String, String>, Map<String, Object>>) resolveQueryAndFilterSQLExpr(leftChildSqlExpr, true, manager);
            Tuple2<Tuple2<String, String>, Map<String, Object>> right = (Tuple2<Tuple2<String, String>, Map<String, Object>>) resolveQueryAndFilterSQLExpr(rightChildSqlExpr, true, manager);
            // query ANDNOT 支持
            if (Constants.Q_AND.equals(opName) && rightChildSqlExpr instanceof SQLBinaryOpExpr && Constants.NOT_LIKE.equals(((SQLBinaryOpExpr) rightChildSqlExpr).getOperator().name_lcase)) {
                opName = Constants.ANDNOT;
            }
            return mergeQueryAndFilter(left, right, opName);
        }
        // 复合表达式仅支持filter
        else if (leftOp) {
            Tuple2<Tuple2<String, String>, Map<String, Object>> left = (Tuple2<Tuple2<String, String>, Map<String, Object>>) resolveQueryAndFilterSQLExpr(leftChildSqlExpr, true, manager);
            String right = getFilterValue(rightChildSqlExpr, manager);
            if (!Constants.EMPTY_STRING.equals(left.t1.t2)) {
                left.t1.t2 = left.t1.t2 + opName + right;
            }
            return left;
        } else {
            Object left = resolveQueryAndFilterSQLExpr(leftChildSqlExpr, false, manager);
            Tuple2<Tuple2<String, String>, Map<String, Object>> right = (Tuple2<Tuple2<String, String>, Map<String, Object>>) resolveQueryAndFilterSQLExpr(rightChildSqlExpr, true, manager);
            if (!Constants.EMPTY_STRING.equals(right.t1.t2)) {
                right.t1.t2 = left +opName + right.t1.t2;
            }
            return right;
        }
//        return EMPTY_STRING_TUPLE2;
    }

    private static Tuple2<Tuple2<String, String>, Map<String, Object>> resolveInnerParam(String name, Object value) {
        Map<String, Object> mp = new HashMap<>();
        if (Constants.FIRST_RANK_NAMES.contains(name)) {
            mp.put(Constants.DEFAULT_FIRST_RANK_NAME, value);
        } else if (Constants.SECOND_RANK_NAMES.contains(name)) {
            mp.put(Constants.DEFAULT_SECOND_RANK_NAME, value);
        } else if (Constants.QUERY_PROCESSOR_NAMES.contains(name)) {
            mp.put(Constants.QUERY_PROCESSOR_NAMES, Collections.singletonList(value));
        } else if (Constants.RE_RANK_SIZE_NAMES.contains(name)) {
            mp.put(Constants.DEFAULT_RE_RANK_SIZE_NAME, value);
        } else if (Constants.DEFAULT_KVPAIRS.equals(name)) {
            mp.put(Constants.DEFAULT_KVPAIRS, value);
        }
        return Tuple2.of(getEmptyStringTuple2(), mp);
    }

    private static Tuple2<Tuple2<String, String>, Map<String, Object>> resolveInnerParam(SQLInListExpr expr, OpenSearchManager manager) {
        String exp = expr.isNot() ? Constants.NOTIN : Constants.IN;
        String name = expr.getExpr() instanceof SQLIdentifierExpr ? ((SQLIdentifierExpr)expr.getExpr()).getLowerName() : resolveQueryAndFilterSQLExpr(expr.getExpr(), manager).toString();
        // 檢查qp是否在in中
        if (Constants.IN.equals(exp)) {
            if (Constants.QUERY_PROCESSOR_NAMES.equals(name)) {
                Map<String, Object> mp = new HashMap<>();
                List<String> qpNames = new ArrayList<>();
                expr.getTargetList().forEach(e -> {
                    if (e instanceof SQLValuableExpr) {
                        qpNames.add(((SQLValuableExpr) e).getValue().toString());
                    }
                });
                if (!qpNames.isEmpty()) {
                    mp.put(Constants.QUERY_PROCESSOR_NAMES, qpNames);
                }
                return Tuple2.of(getEmptyStringTuple2(), mp);
            } else if (Constants.FIRST_RANK_NAMES.contains(name)) {
                Map<String, Object> mp = new HashMap<>();
                expr.getTargetList().stream().findFirst().ifPresent(e -> {
                    if (e instanceof SQLValuableExpr) {
                        mp.put(Constants.DEFAULT_FIRST_RANK_NAME, ((SQLValuableExpr) e).getValue());
                    }
                });
                return Tuple2.of(getEmptyStringTuple2(), mp);
            } else if (Constants.SECOND_RANK_NAMES.contains(name)) {
                Map<String, Object> mp = new HashMap<>();
                expr.getTargetList().stream().findFirst().ifPresent(e -> {
                    if (e instanceof SQLValuableExpr) {
                        mp.put(Constants.DEFAULT_SECOND_RANK_NAME, ((SQLValuableExpr) e).getValue());
                    }
                });
                return Tuple2.of(getEmptyStringTuple2(), mp);
            } else if (Constants.RE_RANK_SIZE_NAMES.contains(name)) {
                Map<String, Object> mp = new HashMap<>();
                expr.getTargetList().stream().findFirst().ifPresent(e -> {
                    if (e instanceof SQLValuableExpr) {
                        mp.put(Constants.DEFAULT_RE_RANK_SIZE_NAME, ((SQLValuableExpr) e).getValue());
                    }
                });
                return Tuple2.of(getEmptyStringTuple2(), mp);
            } else if (Constants.DEFAULT_KVPAIRS.equals(name)) {
                Map<String, Object> mp = new HashMap<>();
                StringBuilder kvpairsBuilder = new StringBuilder();
                expr.getTargetList().forEach(e -> {
                    if (e instanceof SQLValuableExpr) {
                        kvpairsBuilder.append(((SQLValuableExpr) e).getValue()).append(",");
                    }
                });
                if (kvpairsBuilder.length() > 0) {
                    mp.put(Constants.DEFAULT_KVPAIRS, kvpairsBuilder.substring(0, kvpairsBuilder.length() - 1));
                }
                return Tuple2.of(getEmptyStringTuple2(), mp);
            }
        }
        return Tuple2.of(getEmptyStringTuple2(), (Map<String, Object>)Collections.EMPTY_MAP);
    }

    private static Object resolveQueryAndFilterSQLExpr(SQLExpr expr, OpenSearchManager manager) {
        return resolveQueryAndFilterSQLExpr(expr, true, manager);
    }

    private static boolean isInnerParamName(String name) {
        return Constants.INNER_PARAM_NAMES.contains(name);
    }

    /**
     * https://help.aliyun.com/document_detail/204346.html
     * @param binary 是否是表达式类型expr，为false表示表达式一部分的expr，例如SQLMethodInvokeExpr类型获取的结果只是一个值域
     * @return
     */
    private static Object resolveQueryAndFilterSQLExpr(SQLExpr expr, boolean binary, OpenSearchManager manager) {
        // binary
        if (expr instanceof SQLInListExpr) return getQueryAndFilter((SQLInListExpr)expr, manager);
        if (expr instanceof SQLBinaryOpExpr) return getQueryAndFilter((SQLBinaryOpExpr) expr, manager);
        if (expr instanceof SQLBetweenExpr) return getQueryAndFilter((SQLBetweenExpr) expr, manager);

        // maybe binary or not binary, current version is not binary
        if (expr instanceof SQLMethodInvokeExpr) return getQueryAndFilter((SQLMethodInvokeExpr) expr, false, manager);
        // not binary
        if (expr instanceof SQLDefaultExpr) return getQueryAndFilter((SQLDefaultExpr) expr, manager);
        if (expr instanceof SQLIdentifierExpr) return getQueryAndFilter((SQLIdentifierExpr) expr, manager);
        if (expr instanceof SQLNumericLiteralExpr) return getQueryAndFilter((SQLNumericLiteralExpr) expr, manager);
        if (expr instanceof SQLTextLiteralExpr) return getQueryAndFilter((SQLTextLiteralExpr) expr, manager);
        return binary ? Tuple2.of(Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING), new HashMap<>()) : "";
    }

    private static boolean isBinary(SQLExpr expr) {
        if (expr instanceof SQLInListExpr) return true;
        if (expr instanceof SQLBinaryOpExpr)  return true;
        if (expr instanceof SQLBetweenExpr) return true;
        return false;
    }

    private static String getQueryAndFilter(SQLDefaultExpr expr, OpenSearchManager manager) {
        return "default";
    }

    private static String getQueryAndFilter(SQLIdentifierExpr expr, OpenSearchManager manager) {
        return expr.getName();
    }

    private static Object getQueryAndFilter(SQLNumericLiteralExpr expr, OpenSearchManager manager) {
        return expr.getNumber();
    }

    private static String getQueryAndFilter(SQLTextLiteralExpr expr, OpenSearchManager manager) {
        return expr.getText();
    }

    /**
     * https://help.aliyun.com/document_detail/204346.html
     */
    private static String getQueryAndFilter(SQLMethodInvokeExpr expr, boolean op, OpenSearchManager manager) {
        // OpenSearch 函数不支持单引号, 此处需转换
        return expr.toString().replace("'", "\"");
    }

    private static Tuple2<Tuple2<String, String>, Map<String, Object>> getQueryAndFilter(SQLBetweenExpr expr, OpenSearchManager manager) {
        SQLExpr textExpr = expr.testExpr;
        return Tuple2.of(Tuple2.of(Constants.EMPTY_STRING, ((SQLIdentifierExpr) textExpr).getLowerName() + ":["
            + ((SQLValuableExpr) expr.beginExpr).getValue() + "," + ((SQLValuableExpr) expr.endExpr).getValue() + "]"), Collections.emptyMap());
    }

    private static Tuple2<Tuple2<String, String>, Map<String, Object>> getQueryAndFilter(SQLInListExpr expr, OpenSearchManager manager) {
        String exp = expr.isNot() ? Constants.NOTIN : Constants.IN;
        Object name = expr.getExpr() instanceof SQLIdentifierExpr ? ((SQLIdentifierExpr)expr.getExpr()).getLowerName() : resolveQueryAndFilterSQLExpr(expr.getExpr(), manager);
        // 檢查名称是否内部参数名称
        if (isInnerParamName(name.toString())) {
            return resolveInnerParam(expr, manager);
        }
        StringBuilder builder = new StringBuilder(exp + "(" + name + ", \"");
        for (int i = 0; i < expr.getTargetList().size(); i++) {
            if (i > 0) builder.append("|");
            if (expr.getTargetList().get(i) instanceof SQLValuableExpr) {
                builder.append(((SQLValuableExpr) expr.getTargetList().get(i)).getValue());
            }
        }
        builder.append("\")");
        return Tuple2.of(Tuple2.of(Constants.EMPTY_STRING, builder.toString()), Collections.emptyMap());
    }

    private static String getFilterValue(SQLExpr expr, OpenSearchManager manager) {
        if (expr instanceof SQLTextLiteralExpr) {
            return Constants.DOUBLE_QUOTES_MARK + ((SQLTextLiteralExpr) expr).getText() + Constants.DOUBLE_QUOTES_MARK;
        } else if (expr instanceof SQLNumericLiteralExpr) {
            return ((SQLNumericLiteralExpr) expr).getNumber().toString();
        }
        return resolveQueryAndFilterSQLExpr(expr, false, manager).toString();
    }

    private static String getQueryAndFilterBinaryOperatorName(SQLBinaryOperator op) {
        if (Constants.LESS_AND_GREATER.equals(op.name)) {
            return Constants.NE_EQUAL_SIGN;
        }
        return op.name;
    }

    private static Tuple2<Tuple2<String, String>, Map<String, Object>> mergeQueryAndFilter(Tuple2<Tuple2<String, String>, Map<String, Object>> leftTp, Tuple2<Tuple2<String, String>, Map<String, Object>> rightTp, String operatorName) {
        StringBuilder query = new StringBuilder(), filter = new StringBuilder();
        if (!Constants.EMPTY_STRING.equals(leftTp.t1.t1)) {
            query.append(leftTp.t1.t1);
        }
        if (!Constants.EMPTY_STRING.equals(leftTp.t1.t2)) {
            filter.append(leftTp.t1.t2);
        }
        if (!Constants.EMPTY_STRING.equals(rightTp.t1.t1)) {
            if (query.length() > 0) {
                query.insert(0, Constants.SPACE_LEFT_SMALL_BRACKET).append(Constants.SPACE_STRING).append(operatorName).append(Constants.SPACE_STRING)
                    .append(rightTp.t1.t1).append(Constants.RIGHT_SMALL_BRACKET_SPACE);
            } else {
                query.append(rightTp.t1.t1);
            }
        }
        if (!Constants.EMPTY_STRING.equals(rightTp.t1.t2)) {
            if (filter.length() > 0) {
                filter.insert(0, Constants.SPACE_LEFT_SMALL_BRACKET).append(Constants.SPACE_STRING).append(operatorName).append(Constants.SPACE_STRING)
                    .append(rightTp.t1.t2).append(Constants.RIGHT_SMALL_BRACKET_SPACE);
            } else {
                filter.append(rightTp.t1.t2);
            }
        }
        Map<String, Object> map;
        if (leftTp.t2.isEmpty() && rightTp.t2.isEmpty()) {
            map = Collections.emptyMap();
        } else if (!leftTp.t2.isEmpty() && !rightTp.t2.isEmpty()) {
            map = new HashMap<>(leftTp.t2);
            rightTp.t2.forEach((k, v) -> {
                Object mv = map.get(k);
                if (mv == null) {
                    map.put(k, v);
                } else if (mv instanceof Collection && v instanceof Collection){
                    Object instance;
                    if (mv instanceof Set) {
                        instance = new HashSet<>();
                    } else {
                        instance = new ArrayList<>();
                    }
                    ((Collection) instance).addAll((Collection) mv);
                    ((Collection) instance).addAll((Collection) v);
                    map.put(k, instance);
                }
            });
        } else if (!leftTp.t2.isEmpty()) {
            map = leftTp.t2;
        } else {
            map = rightTp.t2;
        }
        return Tuple2.of(Tuple2.of(query.toString(), filter.toString()), map);
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
    public static Sort explainSort(MySqlSelectQueryBlock block, OpenSearchManager manager) {
        if (block.getOrderBy() == null) {
            return null;
        }
        List<SQLSelectOrderByItem> orderByItems = block.getOrderBy().getItems();
        List<SortField> sortFields = new ArrayList<>(orderByItems.size());
        orderByItems.forEach(item -> sortFields.add(
            new SortField(resolveQueryAndFilterSQLExpr(item.getExpr(), false, manager).toString(),
                item.getType() == null || Constants.INCREASE.equals(item.getType().name) ? Order.INCREASE : Order.DECREASE)));
        return !sortFields.isEmpty() ? new Sort(sortFields) : null;
    }

    private static Tuple2<String, String> getEmptyStringTuple2() {
        return Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING);
    }
}


