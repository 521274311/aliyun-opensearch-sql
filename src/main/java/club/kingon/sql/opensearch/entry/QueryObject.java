package club.kingon.sql.opensearch.entry;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

/**
 * @author dragons
 * @date 2021/8/18 21:57
 */
public class QueryObject implements Serializable {
    @JSONField(name = "index_name")
    private String indexName;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }
}
