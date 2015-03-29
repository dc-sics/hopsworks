/*
 */
package io.hops.model;

import java.util.Date;

/**
 *
 * @author Ermias
 */
public interface UsersInterface {
    //Constants to reflect the request status of a user. At creation, status is set to request.
    public static final int STATUS_REQUEST = -1;
    public static final int STATUS_ALLOW = 0;
    
    public String getEmail();

    public String getMobilenum();

    public String getName();

    public Date getRegisteredon();

    public int getStatus();
 
}
