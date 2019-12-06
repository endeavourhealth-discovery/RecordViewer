package models;

public class Request {
    private String httpMethod;

    private int id;

    private Params params;

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getId() {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public Params getParams() {
        return params;
    }

    public void setParams(Params params) {
        this.params = params;
    }
}
