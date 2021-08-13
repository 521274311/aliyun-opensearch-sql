package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author dragons
 * @date 2021/8/12 12:43
 */
public class Field implements Serializable {

    private static final long serialVersionUID = -5809782578272943999L;

    private String name;

    private String type;

    private Boolean primaryKey;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Field field = (Field) o;
        return Objects.equals(name, field.name) && Objects.equals(type, field.type) && Objects.equals(primaryKey, field.primaryKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, primaryKey);
    }
}
