package com.alibaba.csp.sentinel.adapter.dubbo.enums;

/**
 * 字符分隔符枚举
 * <p><br/></p>
 *
 * @author lixiongxing
 * @date 2019-03-07
 */
public enum DelimiterEnums {
    //相关链接字符
    HYPHEN("-", "连接符"),
    CARET("^", "脱字符"),
    TILDE("~", "波浪符"),
    EXCLAMATION("!", "惊叹号"),
    AT("@", "@符"),
    NUMBER("#", "井号"),
    DOLLAR("$", "美元符"),
    PERCENT("%", "百分号"),
    AMPERAND("&", "与和符"),
    ASTERISK("*", "星号"),
    UNDERSCORE("_", "下划线"),
    PLUS("+", "加号"),
    MINUS("-", "减号"),
    EQUALS("=", "等号"),
    LESS("<", "小于号"),
    GREATER(">", "大于号"),
    PREIOD(".", "句号，点"),
    COMMA(",", "逗号"),
    COLON(":", "冒号"),
    SEMICOLON(";", "分号"),
    QUESTION("?", "问号"),
    DASH("–", "破折号"),
    SLASH("/", "斜线"),
    BACKSLASH("\\", "反斜线"),
    VERTICAL("|", "竖线"),
    BRACE("{}", "大括号"),
    LEFT_BRACES("{", "左大括号"),
    RIGHT_BRACE("}", "右大括号"),
    SQUARE("[]","方括号"),
    LEFT_SQUARE("[","左方框号"),
    RIGHT_SQUARE("]","右方框号");


    private String code;

    private String desc;

    private DelimiterEnums(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return this.code;
    }

    public String getDesc() {
        return this.desc;
    }

}
