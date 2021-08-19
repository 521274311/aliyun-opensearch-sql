package club.kingon.sql.opensearch.parser.entry;

import club.kingon.sql.opensearch.SearchQueryModeEnum;
import club.kingon.sql.opensearch.util.Constants;
import com.aliyun.opensearch.sdk.generated.search.*;
import com.aliyun.opensearch.sdk.generated.search.general.SearchResult;

import java.util.List;
import java.util.Set;

/**
 * @author dragons
 * @date 2021/8/13 17:31
 */
public class OpenSearchQueryEntry implements OpenSearchEntry {

    private List<String> appNames;

    private int offset;

    private int count;

    private List<String> fetchField;

    private String query;

    private String filter;

    private Set<Distinct> distincts;

    private Set<Aggregate> aggregates;

    private Sort sort;

    private DeepPaging deepPaging;

    private SearchQueryModeEnum queryMode = SearchQueryModeEnum.HIT;

    private SearchResult result;

    private int batch = Constants.MAX_ONE_HIT;

    private List<String> queryProcessorNames;

    private Rank rank;

    private String kvpairs;

    public List<String> getAppNames() {
        return appNames;
    }

    public void setAppNames(List<String> appNames) {
        this.appNames = appNames;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getFetchField() {
        return fetchField;
    }

    public void setFetchField(List<String> fetchField) {
        this.fetchField = fetchField;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Set<Distinct> getDistincts() {
        return distincts;
    }

    public void setDistincts(Set<Distinct> distincts) {
        this.distincts = distincts;
    }

    public Set<Aggregate> getAggregates() {
        return aggregates;
    }

    public void setAggregates(Set<Aggregate> aggregates) {
        this.aggregates = aggregates;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public DeepPaging getDeepPaging() {
        return deepPaging;
    }

    public void setDeepPaging(DeepPaging deepPaging) {
        this.deepPaging = deepPaging;
    }

    public SearchQueryModeEnum getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(SearchQueryModeEnum queryMode) {
        this.queryMode = queryMode;
    }

    public SearchResult getResult() {
        return result;
    }

    public void setResult(SearchResult result) {
        this.result = result;
    }

    public int getBatch() {
        return batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public List<String> getQueryProcessorNames() {
        return queryProcessorNames;
    }

    public void setQueryProcessorNames(List<String> queryProcessorNames) {
        this.queryProcessorNames = queryProcessorNames;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public String getKvpairs() {
        return kvpairs;
    }

    public void setKvpairs(String kvpairs) {
        this.kvpairs = kvpairs;
    }
}
