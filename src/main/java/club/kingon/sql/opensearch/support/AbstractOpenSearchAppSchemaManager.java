package club.kingon.sql.opensearch.support;

import club.kingon.sql.opensearch.Tuple2;
import club.kingon.sql.opensearch.api.Endpoint;
import club.kingon.sql.opensearch.api.OpenSearchAppNameVersionDetailQueryApiRequest;
import club.kingon.sql.opensearch.api.entry.*;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dragons
 * @date 2021/8/12 13:56
 */
public abstract class AbstractOpenSearchAppSchemaManager extends AbstractOpenSearchAppNameManager {

    private final static Logger log = LoggerFactory.getLogger(AbstractOpenSearchAppSchemaManager.class);

    private volatile Map<String, Schema> versionSchemaMap;

    private volatile Map<String, Indexes> indexesMap;

    protected volatile boolean enableSchemaManagement;

    public AbstractOpenSearchAppSchemaManager(String accessKey, String secret, Endpoint endpoint, String appName) {
        this(accessKey, secret, endpoint, false, appName);
    }

    public AbstractOpenSearchAppSchemaManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName) {
        this(accessKey, secret, endpoint, intranet, appName, true, true);
    }

    public AbstractOpenSearchAppSchemaManager(String accessKey, String secret, Endpoint endpoint, boolean intranet, String appName, boolean enableAppNameManagement, boolean enableSchemaManagement) {
        this(accessKey, secret, endpoint, intranet, appName, enableAppNameManagement, enableSchemaManagement, -1, -1);
    }

    public AbstractOpenSearchAppSchemaManager(String accessKey, String secret, Endpoint endpoint, boolean intranet,
                                              String appName, boolean enableAppNameManagement, boolean enableSchemaManagement,
                                              int connectionTimeout, int readTimeout) {
        super(accessKey, secret, endpoint, intranet, appName, enableAppNameManagement, connectionTimeout, readTimeout);
        this.enableSchemaManagement = enableSchemaManagement;
    }

    @Override
    protected void addTask() {
        addRefreshTask(Tuple2.of((t) -> enableSchemaManagement, (t) -> {
            // 需要存储应用版本信息
            AppName appNameObj = getAppName();
            if (appNameObj != null) {
                Map<String, Schema> newVersionSchemaMap = new HashMap<>();
                if (versionSchemaMap != null) {
                    newVersionSchemaMap.putAll(versionSchemaMap);
                }
                Map<String, Indexes> newIndexesMap = new HashMap<>();
                appNameObj.getVersions().forEach(version -> {
                    try {
                        CommonResponse response = aliyunApiClient.execute(new OpenSearchAppNameVersionDetailQueryApiRequest(appNameObj.getName(), version));
                        // 存储
                        OpenSearchAppNameVersionDetailQueryApiData data = JSON.parseObject(response.getData(), OpenSearchAppNameVersionDetailQueryApiData.class);
                        Schema schema;
                        if (versionSchemaMap == null || (schema = versionSchemaMap.get(version)) == null || schema.hashCode() != data.getResult().getSchema().hashCode()) {
                            newVersionSchemaMap.put(version, data.getResult().getSchema());
                        }
                        // 构建索引
                        Indexes indexes = new Indexes();
                        data.getResult().getSchema().getTables().forEach((k, v) -> {
                            indexes.fieldIndex.putAll(v.getFields());
                        });
                        data.getResult().getSchema().getIndexes().getSearchFields().forEach((k, v) -> {
                            v.getFields().forEach(e -> {
                                List<String> indexNames = indexes.fieldNameIndexIndex.getOrDefault(e, new ArrayList<>());
                                indexNames.add(k);
                                indexes.fieldNameIndexIndex.put(e, indexNames);
                            });
                        });
                        newIndexesMap.put(version, indexes);
                    } catch (ClientException e) {
                        log.error("async invoke opensearch app name struct api fail.", e);
                    }
                });
                versionSchemaMap = newVersionSchemaMap;
                indexesMap = newIndexesMap;
            }
        }), Tuple2.of(null, null));
    }

    @Override
    public Schema getSchema(String version) {
        return emptySchema(version) ? null
            : versionSchemaMap.get(version);
    }

    @Override
    public List<Table> getTables(String version) {
        return emptySchema(version) ? null
            : new ArrayList<>(versionSchemaMap.get(version).getTables().values());
    }

    @Override
    public Table getTable(String version, String tableName) {
        return emptySchema(version) ? null
            : versionSchemaMap.get(version).getTables().get(tableName);
    }

    @Override
    public Field getTableField(String version, String tableName, String fieldName) {
        return emptySchema(version) ? null
            : versionSchemaMap.get(version).getTables().get(tableName).getFields().get(fieldName);
    }

    @Override
    public List<Index> getSearchIndexes(String version) {
        return emptySchema(version) ? null
            : new ArrayList<>(versionSchemaMap.get(version).getIndexes().getSearchFields().values());
    }

    @Override
    public List<String> getSearchIndexNames(String version) {
        return emptySchema(version) ? null
            : new ArrayList<>(versionSchemaMap.get(version).getIndexes().getSearchFields().keySet());
    }

    @Override
    public List<Field> getFields(String version, String tableName) {
        return emptySchema(version) ? null
            : new ArrayList<>(versionSchemaMap.get(version).getTables().get(tableName).getFields().values());
    }

    @Override
    public List<Field> getAllFields(String version) {
        return emptyIndexes(version) ?
            emptySchema(version) ? null
                : versionSchemaMap.get(version).getTables().values().stream().flatMap(t -> t.getFields().values().stream()).collect(Collectors.toList())
            : new ArrayList<>(indexesMap.get(version).fieldIndex.values());
    }

    @Override
    public Field getField(String version, String fieldName) {
        return emptyIndexes(version) ?
            emptySchema(version) ? null
                : versionSchemaMap.get(version).getTables().values().stream().flatMap(t -> t.getFields().values().stream()).filter(f -> fieldName.equals(f.getName())).findFirst().orElse(null)
            : indexesMap.get(version).fieldIndex.get(fieldName);
    }

    @Override
    public List<String> getSearchIndexFieldNames(String version, String indexName) {
        return emptySchema(version) ? null
            : versionSchemaMap.get(version).getIndexes().getSearchFields().get(indexName).getFields();
    }

    @Override
    public List<Field> getSearchIndexFields(String version, String indexName) {
        return emptySchema(version) ? null
            : versionSchemaMap.get(version).getIndexes().getSearchFields().get(indexName).getFields().stream().map(fn -> getField(version, fn)).collect(Collectors.toList());
    }

    @Override
    public List<String> getSearchIndexNamesByFieldName(String version, String fieldName) {
        return emptyIndexes(version) ?
            emptySchema(version) ? null
                : versionSchemaMap.get(version).getIndexes().getSearchFields().entrySet().stream().filter((e -> e.getValue().getFields().contains(fieldName))).map(Map.Entry::getKey).collect(Collectors.toList())
            : indexesMap.get(version).fieldNameIndexIndex.get(fieldName);
    }

    @Override
    public List<String> getFilterAttributeNames(String version) {
        return emptySchema(version) ? null
            : versionSchemaMap.get(version).getIndexes().getFilterFields();
    }

    @Override
    public Index getSearchIndex(String version, String indexName) {
        return emptySchema(version) ? null
            : versionSchemaMap.get(version).getIndexes().getSearchFields().get(indexName);
    }

    private boolean emptySchema(String version) {
        return versionSchemaMap == null || versionSchemaMap.get(version) == null;
    }

    @Override
    public void setEnableAppNameManagement(boolean enableAppNameManagement) {
        super.setEnableAppNameManagement(enableAppNameManagement);
    }

    public boolean isEnableSchemaManagement() {
        return enableSchemaManagement;
    }

    private boolean emptyIndexes(String version) {
        return indexesMap == null || indexesMap.get(version) == null;
    }

    private static class Indexes {

        private final Map<String, Field> fieldIndex = new HashMap<>();

        private final Map<String, List<String>> fieldNameIndexIndex = new HashMap<>();
    }
}
