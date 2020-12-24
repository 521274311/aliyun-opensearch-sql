package club.kingon.sql.opensearch;

/**
 * OpenSearch 查询模式
 * @author dragons
 * @date 2020/12/23 18:22
 */
public enum SearchQueryModeEnum {
    /**
     * Hit 查询模式（前5000条）
     */
    HIT,
    /**
     * Scroll 查询模式（全量）
     */
    SCROLL
}
