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

//    /**
//     * 查询数小于等于5000
//     * @param sql
//     * @return
//     */
//    public static List<SearchParams> querySqlConvert(String sql) {
//        SQLStatement statement = SQLUtils.parseSingleStatement(sql, Constants.MYSQL_DB_TYPE);
//        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
//        statement.accept(visitor);
//        MySqlSelectQueryBlock block = ((MySqlSelectQueryBlock)((SQLSelectStatement) statement).getSelect().getQuery());
//        SQLLimit limit = block.getLimit();
//        List<String> appNames = explainAppNames(visitor);
//        Map<String, String> fetchFieldsAndAlias = explainFetchFieldsAndAlias(block);
//        Tuple2<String, String> queryAndFilter = explainQueryAndFilter((SQLBinaryOpExpr) block.getWhere());
//        Set<Distinct> distinctSet = explainDistinct(block);
//        Set<Aggregate> aggregateSet = explainAggregate(block, visitor);
//        Sort sort = explainSort(block);
//        List<SearchParams> searchParamsList = new ArrayList<>();
//        Tuple2<Integer, Integer> startAndHit = explainStartAndHit(limit);
//        if (startAndHit.t1 + startAndHit.t2 <= Constants.MAX_ALL_HIT) {
//            int start = startAndHit.t1;
//            int hit = startAndHit.t2;
//            Config config;
//            while (hit >= Constants.MAX_ONE_HIT) {
//                config = OpenSearchBuilderUtil.configBuilder()
//                        .appNames(appNames)
//                        .start(start)
//                        .hits(hit)
//                        .searchFormat(SearchFormat.FULLJSON)
//                        .fetchFields(!fetchFieldsAndAlias.isEmpty() ?
//                                new ArrayList<>(fetchFieldsAndAlias.keySet())
//                                : null)
//                        .build();
//                searchParamsList.add(OpenSearchBuilderUtil.searchParamsBuilder(config, !Constants.EMPTY_STRING.equals(queryAndFilter.t1) ? queryAndFilter.t1 : null)
//                        .filter(!Constants.EMPTY_STRING.equals(queryAndFilter.t2) ? queryAndFilter.t2 : null)
//                        .sort(sort)
//                        .distincts(!distinctSet.isEmpty() ? distinctSet : null)
//                        .aggregates(!aggregateSet.isEmpty() ? aggregateSet : null)
//                        .build());
//                start += Constants.MAX_ONE_HIT;
//                hit -= Constants.MAX_ONE_HIT;
//            }
//        } else {
//
//        }
//        return searchParamsList;
//    }

    public static List<String> explainAppNames(MySqlSchemaStatVisitor visitor) {
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
        sqlSelectItemList.forEach(x -> {
            String fieldName = x.getExpr() instanceof SQLIdentifierExpr ? ((SQLIdentifierExpr)x.getExpr()).getLowerName()
                    : ((SQLAggregateExpr) x.getExpr()).getMethodName().toLowerCase();
            result.add(fieldName);
        });
        return result;
    }

    /**
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
     * todo 已完成query与filter提取，Range 提取待完成(https://help.aliyun.com/document_detail/121966.html?spm=5176.15104540.0.dexternal.15fdcc02K1zQyR)
     * 下钻一级递归提取query和filter
     * @return Tuple2<query, filter>
     */
    private static Tuple2<String, String> getQueryAndFilter(SQLBinaryOpExpr expr) {
        SQLExpr leftChildSqlExpr = null, rightChildSqlExpr = null;
        if (expr == null || ((leftChildSqlExpr = expr.getLeft()) == null & (rightChildSqlExpr = expr.getRight()) == null)) {
            return Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING);
        }
        if (!(leftChildSqlExpr instanceof SQLBinaryOpExpr) || !(rightChildSqlExpr instanceof SQLBinaryOpExpr)) {
            if (leftChildSqlExpr instanceof SQLIdentifierExpr && Constants.EQUAL_SIGN.equalsIgnoreCase(expr.getOperator().name)) {
                return Tuple2.of( ((SQLIdentifierExpr) leftChildSqlExpr).getLowerName()
                        + Constants.COLON_SINGLE_QUOTES + ((SQLValuableExpr) rightChildSqlExpr).getValue()
                        + Constants.SINGLE_QUOTES_SPACE, Constants.EMPTY_STRING);
            }
            else if (leftChildSqlExpr instanceof SQLIdentifierExpr && rightChildSqlExpr instanceof SQLCharExpr) {
                 if (Constants.LIKE.equalsIgnoreCase(expr.getOperator().name)) {
                    String value = (String) ((SQLCharExpr) rightChildSqlExpr).getValue();
                    if (value != null && !value.isEmpty()) {
                        if (value.charAt(0) != Constants.PERCENT_SIGN_CHARACTER) {
                            value = Constants.HEAD_TERMINATOR + value.substring(1);
                        }
                        if (value.charAt(value.length() - 1) != Constants.PERCENT_SIGN_CHARACTER) {
                            value = value.substring(0, value.length() - 1) + Constants.TAIL_TERMINATOR;
                        }
                        return Tuple2.of(((SQLIdentifierExpr) leftChildSqlExpr).getLowerName()
                                + Constants.COLON_SINGLE_QUOTES + value + Constants.SINGLE_QUOTES_MARK,
                                Constants.EMPTY_STRING);
                    }
                }
            } else if (leftChildSqlExpr instanceof SQLIdentifierExpr &&
                    (rightChildSqlExpr instanceof SQLIntegerExpr || rightChildSqlExpr instanceof SQLNumberExpr)) {
                return Tuple2.of(Constants.EMPTY_STRING, ((SQLIdentifierExpr) leftChildSqlExpr).getLowerName()
                        + expr.getOperator().name + ((SQLValuableExpr) rightChildSqlExpr).getValue());
            }
        } else {
            Tuple2<String, String> leftTp = getQueryAndFilter((SQLBinaryOpExpr) leftChildSqlExpr);
            Tuple2<String, String> rightTp = getQueryAndFilter((SQLBinaryOpExpr) rightChildSqlExpr);
            StringBuilder query = new StringBuilder(), filter = new StringBuilder();
            if (!Constants.EMPTY_STRING.equals(leftTp.t1)) {
                query.append(leftTp.t1);
            }
            if (!Constants.EMPTY_STRING.equals(leftTp.t2)) {
                filter.append(leftTp.t2);
            }
            if (!Constants.EMPTY_STRING.equals(rightTp.t1)) {
                if (query.length() > 0) {
                    query.insert(0, Constants.SPACE_LEFT_SMALL_BRACKET).append(Constants.SPACE_STRING).append(expr.getOperator().name).append(Constants.SPACE_STRING)
                            .append(rightTp.t1).append(Constants.RIGHT_SMALL_BRACKET_SPACE);
                } else {
                    query.append(rightTp.t1);
                }
            }
            if (!Constants.EMPTY_STRING.equals(rightTp.t2)) {
                if (filter.length() > 0) {
                    filter.insert(0, Constants.SPACE_LEFT_SMALL_BRACKET).append(Constants.SPACE_STRING).append(expr.getOperator().name).append(Constants.SPACE_STRING)
                            .append(rightTp.t2).append(Constants.RIGHT_SMALL_BRACKET_SPACE);
                } else {
                    filter.append(rightTp.t2);
                }
            }
            return Tuple2.of(query.toString(), filter.toString());
        }
        return Tuple2.of(Constants.EMPTY_STRING, Constants.EMPTY_STRING);
    }

    /**
     * https://help.aliyun.com/document_detail/180073.html
     */
    public static Set<Distinct> explainDistinct(MySqlSelectQueryBlock block) {
        boolean globelDistinct = block.getDistionOption() == 2;
        Set<Distinct> distinctSet = new HashSet<>();
        if (globelDistinct) {
            if (block.getSelectList().size() != 1) {
                throw new OpenSearchDqlException("OpenSearch distinct is used, the select field total only be one");
            }
            SQLExpr expr;
            if (! ((expr = block.getSelectList().get(0).getExpr()) instanceof SQLIdentifierExpr)) {
                throw new OpenSearchDqlException("OpenSearch distinct field must be attribute field");
            }
            distinctSet.add(new Distinct(((SQLIdentifierExpr) expr).getLowerName()) {{
                setReserved(false);
                setUpdateTotalHit(true);
            }});
        } else {
            for (SQLSelectItem item : block.getSelectList()) {
                SQLExpr expr = item.getExpr();
                if (expr instanceof SQLAggregateExpr) {
                    SQLAggregateOption option = ((SQLAggregateExpr) expr).getOption();
                    if (option != null && Constants.DISTINCT.equalsIgnoreCase(option.name())) {
                        List<SQLExpr> arguments = ((SQLAggregateExpr) expr).getArguments();
                        if (arguments.size() != 1) {
                            throw new OpenSearchDqlException("OpenSearch distinct is used, the select field total only be one");
                        }
                        if (! ((expr = arguments.get(0)) instanceof SQLIdentifierExpr)) {
                            throw new OpenSearchDqlException("OpenSearch distinct field must be attribute field");
                        }
                        distinctSet.add(new Distinct(((SQLIdentifierExpr) expr).getLowerName()) {{
                            setReserved(false);
                            setUpdateTotalHit(true);
                        }});
                        break;
                    }
                }
            }
        }
        return !distinctSet.isEmpty() ? distinctSet : null;
    }

    /**
     * https://help.aliyun.com/document_detail/180049.html
     */
    public static Set<Aggregate> explainAggregate(MySqlSelectQueryBlock block, MySqlSchemaStatVisitor visitor) {
        Set<TableStat.Column> groupByColumns = visitor.getGroupByColumns();
        List<SQLAggregateExpr> aggregateFunctions = visitor.getAggregateFunctions();
        Aggregate aggregate = null;
        // field
        if (groupByColumns != null && !groupByColumns.isEmpty()) {
            aggregate = new Aggregate();
            StringBuilder groupKeyBuilder = new StringBuilder();
            for (TableStat.Column column : groupByColumns) {
                groupKeyBuilder.append(column.getName()).append(";");
            }
            aggregate.setGroupKey(groupKeyBuilder.substring(0, groupKeyBuilder.length() -1));
        }
        // group by
        if (aggregateFunctions != null && !aggregateFunctions.isEmpty()) {
            if (aggregate == null) {
                throw new OpenSearchDqlException("AggFun must have groupKey.");
            }
            StringBuilder aggFunBuilder = new StringBuilder();
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
                        aggFunBuilder.append(aggFunName).append(argumentName)
                                .append(Constants.LEFT_SMALL_BRACKET)
                                .append(Constants.RIGHT_SMALL_BRACKET).append("#"); break;
                    default:
                        throw new OpenSearchDqlException("AggFun only allow to use count,sum,max,min. Your aggFun: " + aggFunName);
                }
            }
            aggregate.setAggFun(aggFunBuilder.substring(0, aggFunBuilder.length() - 1));
        }
        // having
        Set<Aggregate> aggregateSet = new HashSet<>();
        if (aggregate != null) {
            aggregateSet.add(aggregate);
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


