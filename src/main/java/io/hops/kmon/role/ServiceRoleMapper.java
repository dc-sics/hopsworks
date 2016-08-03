package io.hops.kmon.role;

import io.hops.kmon.struct.RoleType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import io.hops.kmon.struct.ServiceType;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ServiceRoleMapper {

    public static final Map<ServiceType, List<RoleType>> serviceRoleMap;
    public static final Map<RoleType, String> roleFullNames;

    static {
        serviceRoleMap = new EnumMap<ServiceType, List<RoleType>>(ServiceType.class);
        roleFullNames = new EnumMap<RoleType, String>(RoleType.class);

        serviceRoleMap.put(ServiceType.HDFS, Arrays.asList(RoleType.namenode, RoleType.datanode));
        serviceRoleMap.put(ServiceType.NDB, Arrays.asList(RoleType.ndb, RoleType.mysqld, RoleType.mgmserver));
        serviceRoleMap.put(ServiceType.YARN, Arrays.asList(RoleType.resourcemanager, RoleType.nodemanager));

        roleFullNames.put(RoleType.namenode, "Name Node");
        roleFullNames.put(RoleType.datanode, "Data Node");     
        roleFullNames.put(RoleType.ndb, "MySQL Cluster NDB");
        roleFullNames.put(RoleType.mysqld, "MySQL Server");
        roleFullNames.put(RoleType.mgmserver, "MGM Server");
        roleFullNames.put(RoleType.resourcemanager, "Resource Manager");  
        roleFullNames.put(RoleType.nodemanager, "Node Manager");          
    }

    public static List<RoleType> getRoles(ServiceType serviceType) {
        return serviceRoleMap.get(serviceType);
    }   
    
    public static List<RoleType> getRoles(String service) {
        return getRoles(ServiceType.valueOf(service));
    }    
    
    public static String[] getRolesArray(ServiceType serviceType) {
        
        List<RoleType> rolesList = serviceRoleMap.get(serviceType);
        String[] rolesArray = new String[rolesList.size()];
        for (int i=0; i < rolesList.size(); i ++) {
            rolesArray[i] = rolesList.get(i).toString();
        }
        return rolesArray;
    }     
    
    public static String getRoleFullName(RoleType role) {
        return roleFullNames.get(role);
    }
}
