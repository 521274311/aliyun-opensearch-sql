package club.kingon.sql.opensearch.util;

public interface Constants {
    String MYSQL_DB_TYPE = "mysql";
    String EMPTY_STRING = "";
    String EQUAL_SIGN = "=";
    String GREATER_SIGN = ">";
    String GREATER_EQUAL_SIGN = ">=";
    String LESS_SIGN = "<";
    String LESS_EQUAL_SIGN = "<=";
    String SINGLE_QUOTES_MARK = "'";
    String COLON_MARK = ":";
    String SPACE_STRING = " ";
    char PERCENT_SIGN_CHARACTER = '%';
    String LIKE = "like";
    String HEAD_TERMINATOR = "^";
    String TAIL_TERMINATOR = "$";
    String LEFT_SMALL_BRACKET = "(";
    String RIGHT_SMALL_BRACKET = ")";
    String INCREASE = "ASC";
    String DISTINCT = "DISTINCT";
    String COUNT_FUNCTION = "count";
    String MAX_FUNCTION = "max";
    String MIN_FUNCTION = "min";
    String SUM_FUNCTION = "sum";
    int MIN_ONE_START = 0;
    int MAX_ONE_HIT = 500;
    int MAX_ALL_HIT = 5000;
    String COLON_SINGLE_QUOTES = COLON_MARK  + SINGLE_QUOTES_MARK;
    String SINGLE_QUOTES_SPACE = SINGLE_QUOTES_MARK + SPACE_STRING;
    String SPACE_LEFT_SMALL_BRACKET = SPACE_STRING + LEFT_SMALL_BRACKET;
    String RIGHT_SMALL_BRACKET_SPACE = RIGHT_SMALL_BRACKET + SPACE_STRING;

    String SECOND_ABBREVIATION = "s";
    String MINUTE_ABBREVIATION = "m";
    String DAY_ABBREVIATION = "d";
    String HOUR_ABBREVIATION = "h";
    String WEEK_ABBREVIATION = "w";

    String ONE_MINUTE_ABBREVIATION = "1" + MINUTE_ABBREVIATION;
    String FIVE_MINUTE_ABBREVIATION = "5" + MINUTE_ABBREVIATION;
}
