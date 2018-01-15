package io.hops.hopsworks.kmon.role;

import io.hops.hopsworks.kmon.struct.ServiceType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import io.hops.hopsworks.kmon.struct.GroupType;

public class GroupServiceMapper {

  public static final Map<GroupType, List<ServiceType>> groupServiceMap;
  public static final Map<ServiceType, String> serviceFullNames;

  static {
    groupServiceMap = new EnumMap<GroupType, List<ServiceType>>(GroupType.class);
    serviceFullNames = new EnumMap<ServiceType, String>(ServiceType.class);

    groupServiceMap.put(GroupType.HDFS, Arrays.asList(ServiceType.namenode,
            ServiceType.datanode));
    groupServiceMap.put(GroupType.NDB, Arrays.asList(ServiceType.ndbmtd,
            ServiceType.mysqld, ServiceType.ndb_mgmd));
    groupServiceMap.put(GroupType.YARN, Arrays.asList(ServiceType.resourcemanager,
            ServiceType.nodemanager));
    groupServiceMap.put(GroupType.MAP_REDUCE, Arrays.asList(ServiceType.historyserver));
    groupServiceMap.put(GroupType.zookeeper, Arrays.asList(ServiceType.zookeeper));
    groupServiceMap.put(GroupType.influxdb, Arrays.asList(ServiceType.influxdb));
    groupServiceMap.put(GroupType.epipe, Arrays.asList(ServiceType.epipe));
    groupServiceMap.put(GroupType.logstash, Arrays.asList(ServiceType.logstash));
    groupServiceMap.put(GroupType.livy, Arrays.asList(ServiceType.livy));
    groupServiceMap.put(GroupType.historyserver, Arrays.asList(ServiceType.historyserver));
    groupServiceMap.put(GroupType.sparkhistoryserver, Arrays.asList(ServiceType.sparkhistoryserver));
    groupServiceMap.put(GroupType.telegraf, Arrays.asList(ServiceType.telegraf));
    groupServiceMap.put(GroupType.elasticsearch, Arrays.asList(ServiceType.elasticsearch));
    groupServiceMap.put(GroupType.grafana, Arrays.asList(ServiceType.grafana));
    groupServiceMap.put(GroupType.kafka, Arrays.asList(ServiceType.kafka));
    groupServiceMap.put(GroupType.kibana, Arrays.asList(ServiceType.kibana));
    groupServiceMap.put(GroupType.hive, Arrays.asList(ServiceType.hiveserver2));
    groupServiceMap.put(GroupType.hive, Arrays.asList(ServiceType.hivemetastore));
    groupServiceMap.put(GroupType.hive, Arrays.asList(ServiceType.hivecleaner));
    groupServiceMap.put(GroupType.dela, Arrays.asList(ServiceType.dela));
    
    serviceFullNames.put(ServiceType.namenode, "NameNode");
    serviceFullNames.put(ServiceType.datanode, "DataNode");
    serviceFullNames.put(ServiceType.ndbmtd, "MySQL Cluster NDB");
    serviceFullNames.put(ServiceType.mysqld, "MySQL Server");
    serviceFullNames.put(ServiceType.ndb_mgmd, "MGM Server");
    serviceFullNames.put(ServiceType.resourcemanager, "Resource Manager");
    serviceFullNames.put(ServiceType.nodemanager, "Node Manager");
    serviceFullNames.put(ServiceType.zookeeper, "Zookeeper");
    serviceFullNames.put(ServiceType.influxdb, "Influxdb");
    serviceFullNames.put(ServiceType.epipe, "Epipe");
    serviceFullNames.put(ServiceType.logstash, "Logstash");
    serviceFullNames.put(ServiceType.livy, "Livy");
    serviceFullNames.put(ServiceType.historyserver, "MapRed History Server");
    serviceFullNames.put(ServiceType.sparkhistoryserver, "Spark History Server");
    serviceFullNames.put(ServiceType.telegraf, "Telegraf");
    serviceFullNames.put(ServiceType.elasticsearch, "Elasticsearch");
    serviceFullNames.put(ServiceType.grafana, "Grafana");
    serviceFullNames.put(ServiceType.kafka, "Kafka");
    serviceFullNames.put(ServiceType.kibana, "Kibana");
    serviceFullNames.put(ServiceType.hiveserver2, "HiveServer2");
    serviceFullNames.put(ServiceType.hivemetastore, "HiveMetastore");
    serviceFullNames.put(ServiceType.hivecleaner, "HiveCleaner");
    serviceFullNames.put(ServiceType.dela, "Dela");
  }

  public static List<ServiceType> getServices(GroupType groupType) {
    return groupServiceMap.get(groupType);
  }

  public static List<ServiceType> getServices(String group) {
    return GroupServiceMapper.getServices(GroupType.valueOf(group));
  }

  public static String[] getServicesArray(GroupType serviceType) {

    List<ServiceType> rolesList = groupServiceMap.get(serviceType);
    String[] servicesArray = new String[rolesList.size()];
    for (int i = 0; i < rolesList.size(); i++) {
      servicesArray[i] = rolesList.get(i).toString();
    }
    return servicesArray;
  }

  public static String getServiceFullName(ServiceType role) {
    return serviceFullNames.get(role);
  }
}
