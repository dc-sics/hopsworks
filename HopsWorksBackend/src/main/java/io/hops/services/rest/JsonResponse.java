package io.hops.services.rest;

import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@XmlRootElement //we don't need this thanks to Jackson
//@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)  
//@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)  
public class JsonResponse{
     
    private static final float version = 1.0f;  
 
    private String status;
    private Integer statusCode;
    private String errorMsg;
    private Map<String, Object> fieldErrors;
    private Object data;
    private String sessionID;
 
    public JsonResponse() {
    }
     
    public JsonResponse(String status) {
        this.status = status;
    }  

    public JsonResponse(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    
     
    @XmlElement
    public float getVersion() {
        return JsonResponse.version;
    }
         
    public String getStatus() {
        return status;
    }
 
    public void setStatus(String status) {
        this.status = status;
    }
      @XmlElement
    public String getErrorMsg() {
        return errorMsg;
    }
 
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
 
    public Map<String, Object> getFieldErrors() {
        return fieldErrors;
    }
 
    public void setFieldErrors(Map<String, Object> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
     @XmlElement
    public Object getData() {
        return data;
    }
 
    public void setData(Object data) {
        this.data = data;
    }
    
    @XmlElement
    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }
     
}