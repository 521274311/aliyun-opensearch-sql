package club.kingon.sql.opensearch.entry;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.util.List;

/**
 * @author dragons
 * @date 2021/8/18 18:05
 */
public class QueryResult<T> implements Serializable {

    private Double searchtime;

    private Long total;

    private Integer num;

    private Integer viewtotal;

    private List<Item<T>> items;

    private List<Group> facet;

    public Double getSearchtime() {
        return searchtime;
    }

    public void setSearchtime(Double searchtime) {
        this.searchtime = searchtime;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Integer getViewtotal() {
        return viewtotal;
    }

    public void setViewtotal(Integer viewtotal) {
        this.viewtotal = viewtotal;
    }

    public List<Item<T>> getItems() {
        return items;
    }

    public void setItems(List<Item<T>> items) {
        this.items = items;
    }

    public List<Group> getFacet() {
        return facet;
    }

    public void setFacet(List<Group> facet) {
        this.facet = facet;
    }

    public static class Item<T> {
        private T fields;

        private JSONObject property;

        private JSONObject attribute;

        private JSONObject variableValue;

        private List<String> sortExprValues;

        public T getFields() {
            return fields;
        }

        public void setFields(T fields) {
            this.fields = fields;
        }

        public JSONObject getProperty() {
            return property;
        }

        public void setProperty(JSONObject property) {
            this.property = property;
        }

        public JSONObject getAttribute() {
            return attribute;
        }

        public void setAttribute(JSONObject attribute) {
            this.attribute = attribute;
        }

        public JSONObject getVariableValue() {
            return variableValue;
        }

        public void setVariableValue(JSONObject variableValue) {
            this.variableValue = variableValue;
        }

        public List<String> getSortExprValues() {
            return sortExprValues;
        }

        public void setSortExprValues(List<String> sortExprValues) {
            this.sortExprValues = sortExprValues;
        }

        @Override
        public String toString() {
            return "Item{" +
                "fields=" + fields +
                ", property=" + property +
                ", attribute=" + attribute +
                ", variableValue=" + variableValue +
                ", sortExprValues=" + sortExprValues +
                '}';
        }
    }

    @Override
    public String toString() {
        return "QueryResult{" +
            "searchtime=" + searchtime +
            ", total=" + total +
            ", num=" + num +
            ", viewtotal=" + viewtotal +
            ", items=" + items +
            ", facet=" + facet +
            '}';
    }
}
