package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author dragons
 * @date 2021/8/12 12:45
 */
public class Index implements Serializable {

    private static final long serialVersionUID = -5809782578272943999L;

    private String analyzer;

    private List<String> fields;

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index index = (Index) o;
        return Objects.equals(analyzer, index.analyzer) && Objects.equals(fields, index.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analyzer, fields);
    }
}
