/*
 */
package io.hops.model;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Ermias
 */
@XmlRootElement
public interface UsersInterface {
    //Constants to reflect the request status of a user. At creation, status is set to request.
    public static final int STATUS_REQUEST = -1;
    public static final int STATUS_ALLOW = 0;
    
    public static final int STATUS_IS_ONLINE = 0;
    public static final int STATUS_IS_OFFLINE = -1;
    
    
    public String getEmail();

    public String getTelephoneNum();

    public String getFirstName();
    public String getLastName();

    public Date getRegisteredon();

    public int getStatus();
 
}
