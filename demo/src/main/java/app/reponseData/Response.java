package app.reponseData;

/**
 * 响应登录
 */
public class Response {
    //响应代码
    private int code;
    //描述信息
    private String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public  Response(int c, String m){
        code = c;
        message = m;
    }
}
