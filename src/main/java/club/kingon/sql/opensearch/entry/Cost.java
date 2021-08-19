package club.kingon.sql.opensearch.entry;

import java.io.Serializable;

/**
 * @author dragons
 * @date 2021/8/18 18:07
 */
public class Cost implements Serializable {

    private String indexName;

    private Double value;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
