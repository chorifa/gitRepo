package protostuffDemo;

import java.util.Date;

public class RequestExample {

    private Long requestId;

    private String requestMethod;

    private Date requestTime;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    @Override
    public String toString() {
        return "RequestExample{" +
                "requestId=" + requestId +
                ", requestMethod='" + requestMethod + '\'' +
                ", requestTime=" + requestTime +
                '}';
    }
}
