package club.kingon.sql.opensearch.util;

import com.aliyun.opensearch.sdk.generated.search.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>OpenSearch 配置建造器工具</p>
 * @author dragons
 * @date 2020/12/22 18:40
 */
public class OpenSearchBuilderUtil {

    public static class ConfigBuilder {
        private List<String> appNames;
        private int start = 0;
        private int hits = 10;
        private SearchFormat searchFormat = SearchFormat.FULLJSON;
        private List<String> fetchFields;
        private String kvpairs;
        private List<String> customConfig;
        private String routeValue;

        private ConfigBuilder() {}

        private ConfigBuilder(List<String> appNames, int start, int hit, List<String> fetchFields) {
            this.appNames = appNames;
            this.start = start;
            this.hits = hit;
            this.fetchFields = fetchFields;
        }

        public ConfigBuilder appNames(List<String> appNames) {
            this.appNames = appNames;
            return this;
        }

        public ConfigBuilder start(int start) {
            this.start = start;
            return this;
        }

        public ConfigBuilder hits(int hits) {
            this.hits = hits;
            return this;
        }

        public ConfigBuilder searchFormat(SearchFormat searchFormat) {
            this.searchFormat = searchFormat;
            return this;
        }

        public ConfigBuilder fetchFields(List<String> fetchFields) {
            this.fetchFields = fetchFields;
            return this;
        }

        public ConfigBuilder fetchFields(String kvpairs) {
            this.kvpairs = kvpairs;
            return this;
        }

        public ConfigBuilder customConfig(List<String> customConfig) {
            this.customConfig = customConfig;
            return this;
        }

        public ConfigBuilder routeValue(String routeValue) {
            this.routeValue = routeValue;
            return this;
        }

        public ConfigBuilder kvpairs(String kvpairs) {
            this.kvpairs = kvpairs;
            return this;
        }

        public Config build() {
            Config cfg = new Config(appNames);
            cfg.setStart(start);
            cfg.setHits(hits);
            cfg.setSearchFormat(searchFormat);
            cfg.setFetchFields(fetchFields);
            cfg.setKvpairs(kvpairs);
            cfg.setCustomConfig(customConfig);
            cfg.setRouteValue(routeValue);
            return cfg;
        }
    }

    public static ConfigBuilder configBuilder() {
        return new ConfigBuilder();
    }

    public static ConfigBuilder configBuilder(List<String> appNames, int start, int hit, List<String> fetchFields) {
        return new ConfigBuilder(appNames, start, hit, fetchFields);
    }

    public static class SearchParamsBuilder {
        private Config config;
        private String query;
        private String filter;
        private Sort sort;
        private Rank rank = new Rank() {{
            setReRankSize(200);
        }};
        private Set<Aggregate> aggregates;
        private Set<Distinct> distincts;
        private Set<Summary> summaries;
        private List<String> queryProcessorNames;
        private DeepPaging deepPaging;
        private Map<String, String> disableFunctions;
        private Map<String, String> customParam;
        private Suggest suggest;

        private SearchParamsBuilder() {
        }

        private SearchParamsBuilder(Config config, String query) {
            this.config = config;
            this.query = query;
        }

        private SearchParamsBuilder(SearchParams searchParams) {
            searchParams(searchParams);
        }

        public SearchParamsBuilder searchParams(SearchParams searchParams) {
            this.config = searchParams.getConfig();
            this.query = searchParams.getQuery();
            this.filter = searchParams.getFilter();
            this.sort = searchParams.getSort();
            this.rank = searchParams.getRank();
            this.aggregates = searchParams.getAggregates();
            this.distincts = searchParams.getDistincts();
            this.summaries = searchParams.getSummaries();
            this.queryProcessorNames = searchParams.getQueryProcessorNames();
            this.deepPaging = searchParams.getDeepPaging();
            this.disableFunctions = searchParams.getDisableFunctions();
            this.customParam = searchParams.getCustomParam();
            this.suggest = searchParams.getSuggest();
            return this;
        }

        public SearchParamsBuilder config(Config config) {
            this.config = config;
            return this;
        }

        public SearchParamsBuilder query(String query) {
            this.query = query;
            return this;
        }

        public SearchParamsBuilder filter(String filter) {
            this.filter = filter;
            return this;
        }

        public SearchParamsBuilder sort(Sort sort) {
            this.sort = sort;
            return this;
        }

        public SearchParamsBuilder rank(Rank rank) {
            this.rank = rank;
            return this;
        }

        public SearchParamsBuilder aggregates(Set<Aggregate> aggregates) {
            this.aggregates = aggregates;
            return this;
        }

        public SearchParamsBuilder distincts(Set<Distinct> distincts) {
            this.distincts = distincts;
            return this;
        }

        public SearchParamsBuilder summaries(Set<Summary> summaries) {
            this.summaries = summaries;
            return this;
        }

        public SearchParamsBuilder queryProcessorNames(List<String> queryProcessorNames) {
            this.queryProcessorNames = queryProcessorNames;
            return this;
        }

        public SearchParamsBuilder deepPaging(DeepPaging deepPaging) {
            this.deepPaging = deepPaging;
            return this;
        }

        public SearchParamsBuilder disableFunctions(Map<String, String> disableFunctions) {
            this.disableFunctions = disableFunctions;
            return this;
        }

        public SearchParamsBuilder customParam(Map<String, String> customParam) {
            this.customParam = customParam;
            return this;
        }

        public SearchParamsBuilder suggest(Suggest suggest) {
            this.suggest = suggest;
            return this;
        }

        public SearchParams build() {
            SearchParams searchParams = new SearchParams(config);
            searchParams.setQuery(query);
            searchParams.setFilter(filter);
            searchParams.setSort(sort);
            searchParams.setRank(rank);
            searchParams.setAggregates(aggregates);
            searchParams.setDistincts(distincts);
            searchParams.setSummaries(summaries);
            searchParams.setQueryProcessorNames(queryProcessorNames);
            searchParams.setDeepPaging(deepPaging);
            searchParams.setDisableFunctions(disableFunctions);
            searchParams.setCustomParam(customParam);
            searchParams.setSuggest(suggest);
            return searchParams;
        }
    }

    public static SearchParamsBuilder searchParamsBuilder() {
        return new SearchParamsBuilder();
    }

    public static SearchParamsBuilder searchParamsBuilder(Config config, String query) {
        return new SearchParamsBuilder(config, query);
    }

    public static SearchParamsBuilder searchParamsBuilder(SearchParams searchParams) {
        return new SearchParamsBuilder(searchParams);
    }
}
