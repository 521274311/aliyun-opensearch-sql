package club.kingon.sql.opensearch.parser;

import club.kingon.sql.opensearch.OpenSearchDqlException;
import club.kingon.sql.opensearch.OpenSearchManager;
import club.kingon.sql.opensearch.SearchQueryModeEnum;
import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.parser.entry.OpenSearchEntry;
import club.kingon.sql.opensearch.parser.entry.OpenSearchQueryEntry;
import club.kingon.sql.opensearch.util.Constants;
import club.kingon.sql.opensearch.parser.util.OpenSearchSqlConverter;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.aliyun.opensearch.sdk.generated.search.DeepPaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author dragons
 * @date 2021/8/13 17:26
 */
public class DefaultOpenSearchQuerySQLParser extends AbstractOpenSearchSQLParser {

    protected MySqlSelectQueryBlock block;

    private final static Logger log = LoggerFactory.getLogger(DefaultOpenSearchQuerySQLParser.class);

    public DefaultOpenSearchQuerySQLParser(String sql, OpenSearchManager manager) {
        super(sql, manager);
        block = ((MySqlSelectQueryBlock)((SQLSelectStatement) statement).getSelect().getQuery());
    }

    @Override
    @SuppressWarnings("uncheck")
    public <T extends OpenSearchEntry> T parse(String sql) {
        OpenSearchQueryEntry config = new OpenSearchQueryEntry();
        config.setAppNames(OpenSearchSqlConverter.explainFrom(visitor));
        config.setFetchField(OpenSearchSqlConverter.explainFetchField(block));
        Tuple2<Tuple2<String, String>, Map<String, Object>> queryAndFilterAndParams = OpenSearchSqlConverter.explainWhere(block.getWhere(), manager);
        config.setQuery(queryAndFilterAndParams.t1.t1);
        config.setFilter(queryAndFilterAndParams.t1.t2);
        config.setQueryProcessorNames((List<String>) queryAndFilterAndParams.t2.get(Constants.QUERY_PROCESSOR_NAMES));
        config.setRank(OpenSearchSqlConverter.explainRank(queryAndFilterAndParams.t2));
        config.setKvpairs((String) queryAndFilterAndParams.t2.get(Constants.DEFAULT_KVPAIRS));
        config.setDistincts(OpenSearchSqlConverter.explainDistinct(block));
        config.setAggregates(OpenSearchSqlConverter.explainAggregate(block, visitor, queryAndFilterAndParams.t1.t2, manager));
        config.setSort(OpenSearchSqlConverter.explainSort(block, manager));
        Tuple2<Integer, Integer> offsetAndCount = OpenSearchSqlConverter.explainStartAndHit(block.getLimit());
        if (offsetAndCount.t2 == null) {
            config.setQueryMode(SearchQueryModeEnum.SCROLL);
            config.setCount(Integer.MAX_VALUE);
            DeepPaging deepPaging = new DeepPaging();
            deepPaging.setScrollExpire(Constants.FIVE_MINUTE_ABBREVIATION);
            config.setDeepPaging(deepPaging);
        } else {
            if (offsetAndCount.t1 == null) {
                offsetAndCount.t1 = Constants.MIN_ONE_START;
            }
            if (offsetAndCount.t1 >= Constants.MAX_ALL_HIT) {
                throw new OpenSearchDqlException("limit first param must be less than " + Constants.MAX_ALL_HIT);
            }
            if (offsetAndCount.t1 + offsetAndCount.t2 > Constants.MAX_ALL_HIT) {
                log.warn("your limit value already exceeds opensearch hit limit that max(offset + hit = {} + {}). the upper limit value hit is automatically changed to {} here.",
                        offsetAndCount.t1, offsetAndCount.t2, (offsetAndCount.t2 = Constants.MAX_ALL_HIT - offsetAndCount.t1));
            }
            config.setOffset(offsetAndCount.t1);
            config.setCount(offsetAndCount.t2);
        }
        return (T) config;
    }
}
