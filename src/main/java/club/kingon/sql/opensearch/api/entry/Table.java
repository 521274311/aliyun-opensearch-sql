package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author dragons
 * @date 2021/8/12 12:44
 */
public class Table implements Serializable {

    private static final long serialVersionUID = -5809782578272943999L;

    private String name;

    private Map<String, Field> fields;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    public void setFields(Map<String, Field> fields) {
        this.fields = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return Objects.equals(name, table.name) && Objects.equals(fields, table.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, fields);
    }
}
