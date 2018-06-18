package model;

import org.apache.commons.lang.StringUtils;

/**
 * code-data的返回格式
 * code进行错误判断
 * 数据存储在data内
 */
public class JsonMsg {
    private String code;
    private Object data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public static void main(String[] args) {
        String[] ss= StringUtils.split("  "," ");

        System.out.println(" ");
        System.out.println(" ");
    }
}
