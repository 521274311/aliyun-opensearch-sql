package club.kingon.sql.opensearch.util;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLLimit;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
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
 * 1.目前仅支持查询功能
 * 2.当前支持语法
 *  2.1.暂不支持函数模式。
 *  2.2.select仅支持查询字段（暂不支持去重(distinct)，聚合(max,count,min,avg))
 *  2.3.暂不支持group by语法
 *  2.4.where中暂不支持 in 语法以及嵌套查询
 * @author dragons
 * @date 2020/12/18 16:13
 */
public class OpenSearchParser {

    public static SearchParams queryParser(String sql) {
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, Constants.MYSQL_DB_TYPE);
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        statement.accept(visitor);
        MySqlSelectQueryBlock block = ((MySqlSelectQueryBlock)((SQLSelectStatement) statement).getSelect().getQuery());
        SQLLimit limit = block.getLimit();
        List<String> appNames = explainAppNames(visitor);
        Tuple2<Integer, Integer> startAndHits = explainStartAndHits(limit);
        Map<String, String> fetchFieldsAndAlias = explainFetchFieldsAndAlias(block);
        Tuple2<String, String> queryAndFilter = explainQueryAndFilter((SQLBinaryOpExpr) block.getWhere());
        Sort sort = explainSort(block);
        Config config = OpenSearchBuilderUtil.configBuilder()
                .appNames(appNames)
                .start(startAndHits.t1)
                .hits(startAndHits.t2)
                .searchFormat(SearchFormat.FULLJSON)
                .fetchFields(new ArrayList<>(fetchFieldsAndAlias.keySet()))
                .build();
        return OpenSearchBuilderUtil.searchParamsBuilder(config, !Constants.EMPTY_STRING.equals(queryAndFilter.t1) ? queryAndFilter.t1 : null)
                .filter(!Constants.EMPTY_STRING.equals(queryAndFilter.t2) ? queryAndFilter.t2 : null)
                .sort(sort)
                .build();
    }

    private static List<String> explainAppNames(MySqlSchemaStatVisitor visitor) {
        Set<TableStat.Name> names = visitor.getTables().keySet();
        List<String> appNames = new ArrayList<>(names.size());
        names.forEach(n -> appNames.add(n.getName()));
        return appNames;
    }

    private static Tuple2<Integer, Integer> explainStartAndHits(SQLLimit limit) {
        int offset = 0, count = 10;
        if (limit != null) {
            if (limit.getOffset() != null) {
                offset = (int) ((SQLIntegerExpr) limit.getOffset()).getNumber();
            }
            if (limit.getRowCount() != null) {
                count = (int) ((SQLIntegerExpr) limit.getRowCount()).getNumber();
            }
        }
        return Tuple2.of(offset, count);
    }

    /**
     * @return Map<index_name, alias_name>
     */
    private static Map<String, String> explainFetchFieldsAndAlias(MySqlSelectQueryBlock block) {
        List<SQLSelectItem> sqlSelectItemList = block.getSelectList();
        if (sqlSelectItemList == null || sqlSelectItemList.size() <= 0) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        sqlSelectItemList.forEach(x -> result.put(((SQLIdentifierExpr)x.getExpr()).getLowerName(),
                x.getAlias() != null && !Constants.EMPTY_STRING.equals(x.getAlias()) ? x.getAlias()
                        : ((SQLIdentifierExpr)x.getExpr()).getLowerName()));
        return result;
    }

    /**
     * todo 已完成query与filter提取，Range 提取待完成(https://help.aliyun.com/document_detail/121966.html?spm=5176.15104540.0.dexternal.15fdcc02K1zQyR)
     * 下钻一级递归提取query和filter
     * @return Tuple2<query, filter>
     */
    private static Tuple2<String, String> explainQueryAndFilter(SQLBinaryOpExpr expr) {
        SQLExpr leftChildSqlExpr = expr.getLeft(), rightChildSqlExpr = expr.getRight();
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
                        if (value.charAt(0) == Constants.PERCENT_SIGN_CHARACTER) {
                            value = Constants.HEAD_TERMINATOR + value.substring(1);
                        }
                        if (value.charAt(value.length() - 1) == Constants.PERCENT_SIGN_CHARACTER) {
                            value = value.substring(0, value.length() - 1) + Constants.TAIL_TERMINATOR;
                        }
                        return Tuple2.of(((SQLIdentifierExpr) leftChildSqlExpr).getLowerName()
                                + Constants.COLON_SINGLE_QUOTES + value + Constants.SINGLE_QUOTES_SPACE,
                                Constants.EMPTY_STRING);
                    }
                }
            } else if (leftChildSqlExpr instanceof SQLIdentifierExpr &&
                    (rightChildSqlExpr instanceof SQLIntegerExpr || rightChildSqlExpr instanceof SQLNumberExpr)) {
                return Tuple2.of(Constants.EMPTY_STRING, ((SQLIdentifierExpr) leftChildSqlExpr).getLowerName()
                        + expr.getOperator().name + ((SQLValuableExpr) rightChildSqlExpr).getValue() + Constants.SPACE_STRING);
            }
        } else {
            Tuple2<String, String> leftTp = explainQueryAndFilter((SQLBinaryOpExpr) leftChildSqlExpr);
            Tuple2<String, String> rightTp = explainQueryAndFilter((SQLBinaryOpExpr) rightChildSqlExpr);
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

    private static Sort explainSort(MySqlSelectQueryBlock block) {
        if (block.getOrderBy() == null) {
            return null;
        }
        List<SQLSelectOrderByItem> orderByItems = block.getOrderBy().getItems();
        List<SortField> sortFields = new ArrayList<>(orderByItems.size());
        orderByItems.forEach(item -> sortFields.add(new SortField(((SQLIdentifierExpr)item.getExpr()).getLowerName(),
                item.getType() == null || Constants.INCREASE.equalsIgnoreCase(item.getType().name) ? Order.INCREASE : Order.DECREASE)));
        return new Sort(sortFields);
    }
}


