package club.kingon.sql.opensearch.api.entry;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author dragons
 * @date 2021/8/12 12:46
 */
public class Schema implements Serializable {

    private static final long serialVersionUID = -5809782578272943999L;

    private Map<String, Table> tables;

    private Indexes indexes;

    public Map<String, Table> getTables() {
        return tables;
    }

    public void setTables(Map<String, Table> tables) {
        this.tables = tables;
    }

    public Indexes getIndexes() {
        return indexes;
    }

    public void setIndexes(Indexes indexes) {
        this.indexes = indexes;
    }

    public static class Indexes implements Serializable {
        private static final long serialVersionUID = -5809782578272943999L;

        private Map<String, Index> searchFields;

        private List<String> filterFields;

        public Map<String, Index> getSearchFields() {
            return searchFields;
        }

        public void setSearchFields(Map<String, Index> searchFields) {
            this.searchFields = searchFields;
        }

        public List<String> getFilterFields() {
            return filterFields;
        }

        public void setFilterFields(List<String> filterFields) {
            this.filterFields = filterFields;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Indexes indexes = (Indexes) o;
            return Objects.equals(searchFields, indexes.searchFields) && Objects.equals(filterFields, indexes.filterFields);
        }

        @Override
        public int hashCode() {
            return Objects.hash(searchFields, filterFields);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schema schema = (Schema) o;
        return Objects.equals(tables, schema.tables) && Objects.equals(indexes, schema.indexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tables, indexes);
    }
}
