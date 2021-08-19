package club.kingon.sql.opensearch.entry;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author dragons
 * @date 2021/8/18 18:10
 */
public class Group implements Serializable {

    private String key;

    private List<Item> items;

    public static class Item implements Serializable {
        private BigDecimal min;

        private BigDecimal max;

        private Long count;

        private BigDecimal sum;

        private Object value;

        public BigDecimal getMin() {
            return min;
        }

        public void setMin(BigDecimal min) {
            this.min = min;
        }

        public BigDecimal getMax() {
            return max;
        }

        public void setMax(BigDecimal max) {
            this.max = max;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public BigDecimal getSum() {
            return sum;
        }

        public void setSum(BigDecimal sum) {
            this.sum = sum;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
