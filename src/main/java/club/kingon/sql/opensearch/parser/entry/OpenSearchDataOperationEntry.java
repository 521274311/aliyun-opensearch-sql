package club.kingon.sql.opensearch.parser.entry;

import java.util.List;
import java.util.Map;

/**
 * @author dragons
 * @date 2021/8/19 15:59
 */
public class OpenSearchDataOperationEntry implements OpenSearchEntry {

    private String appName;

    private String tableName;

    private List<Map<String, Object>> data;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }
}
