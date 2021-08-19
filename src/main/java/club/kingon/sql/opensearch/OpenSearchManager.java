package club.kingon.sql.opensearch;


import club.kingon.sql.opensearch.api.Endpoint;
import club.kingon.sql.opensearch.api.entry.*;

import java.util.List;

/**
 * @author dragons
 * @date 2021/8/12 11:28
 */
public interface OpenSearchManager extends OpenSearchResourceManager {

    String getAccessKey();

    String getSecret();

    Endpoint getEndpoint();

    AppName getAppName();

    String getMasterVersion();

    List<String> getVersions();

    default Schema getSchema() {
        return getSchema(getMasterVersion());
    }

    Schema getSchema(String version);

    default List<Table> getTables() {
        return getTables(getMasterVersion());
    }

    List<Table> getTables(String version);

    default Table getPrimaryTable() {
        return getPrimaryTable(getMasterVersion());
    }

    default Table getPrimaryTable(String version) {
        List<Table> tables = getTables(version);
        for (Table table : tables) {
            if (table.getPrimaryTable()) {
                return table;
            }
        }
        return null;
    }

    default Table getTable(String tableName) {
        return getTable(getMasterVersion(), tableName);
    }

    Table getTable(String version, String tableName);

    default Field getTableField(String tableName, String fieldName) {
        return getTableField(getMasterVersion(), tableName, fieldName);
    }

    Field getTableField(String version, String tableName, String fieldName);

    default List<Index> getSearchIndexes() {
        return getSearchIndexes(getMasterVersion());
    }

    List<Index> getSearchIndexes(String version);

    default List<String> getSearchIndexNames() {
        return getSearchIndexNames(getMasterVersion());
    }

    List<String> getSearchIndexNames(String version);

    default List<Field> getFields(String tableName) {
        return getFields(getMasterVersion(), tableName);
    }

    List<Field> getFields(String version, String tableName);

    default List<Field> getAllFields() {
        return getAllFields(getMasterVersion());
    }

    List<Field> getAllFields(String version);

    default Field getField(String fieldName) {
        return getField(getMasterVersion(), fieldName);
    }

    Field getField(String version, String fieldName);

    default List<String> getSearchIndexFieldNames(String indexName) {
        return getSearchIndexFieldNames(getMasterVersion(), indexName);
    }

    default Index getSearchIndex(String indexName) {
        return getSearchIndex(getMasterVersion(), indexName);
    }

    Index getSearchIndex(String version, String indexName);

    List<String> getSearchIndexFieldNames(String version, String indexName);

    default List<Field> getSearchIndexFields(String indexName) {
        return getSearchIndexFields(getMasterVersion(), indexName);
    }

    List<Field> getSearchIndexFields(String version, String indexName);

    default List<String> getSearchIndexNamesByFieldName(String fieldName) {
        return getSearchIndexNamesByFieldName(getMasterVersion(), fieldName);
    }

    List<String> getSearchIndexNamesByFieldName(String version, String fieldName);

    default List<String> getFilterAttributeNames() {
        return getFilterAttributeNames(getMasterVersion());
    }

    List<String> getFilterAttributeNames(String version);

    default boolean existsField(String fieldName) {
        return existsField(getMasterVersion(), fieldName);
    }

    default boolean existsField(String version, String fieldName) {
        return getField(version, fieldName) != null;
    }

    default boolean existsTable(String tableName) {
        return existsTable(getMasterVersion(), tableName);
    }

    default boolean existsTable(String version, String tableName) {
        return getTable(version, tableName) != null;
    }

    default boolean existsSearchIndex(String indexName) {
        return existsSearchIndex(getMasterVersion(), indexName);
    }

    default boolean existsSearchIndex(String version, String indexName) {
        List<String> searchIndexes = getSearchIndexNames(version);
        return searchIndexes != null && searchIndexes.contains(indexName);
    }

    default boolean existsFilterAttribute(String attributeName) {
        return existsFilterAttribute(getMasterVersion(), attributeName);
    }

    default boolean existsFilterAttribute(String version, String attributeName) {
        List<String> filterAttributeNames = getFilterAttributeNames(version);
        return filterAttributeNames != null && filterAttributeNames.contains(attributeName);
    }
}
