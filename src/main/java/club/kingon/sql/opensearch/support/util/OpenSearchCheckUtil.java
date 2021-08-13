package club.kingon.sql.opensearch.support.util;

import club.kingon.sql.opensearch.api.entry.Field;

import java.util.List;

/**
 * @author dragons
 * @date 2021/8/12 17:26
 */
public class OpenSearchCheckUtil {

    public static boolean supportFuzzyQuery(List<Field> fields) {
        if (fields == null || fields.isEmpty()) return false;

        for (Field field : fields) {
            if (!"SHORT_TEXT".equalsIgnoreCase(field.getType())) return false;
        }
        return true;
    }
}
