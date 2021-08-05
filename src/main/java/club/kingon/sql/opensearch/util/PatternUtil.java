package club.kingon.sql.opensearch.util;

import java.util.regex.Pattern;

/**
 * @author dragons
 * @date 2021/8/5 11:01
 */
public class PatternUtil {

    private final static Pattern CONTAINS_CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]");

    public static boolean hasChinese(String v) {
        return CONTAINS_CHINESE.matcher(v).find();
    }
}
