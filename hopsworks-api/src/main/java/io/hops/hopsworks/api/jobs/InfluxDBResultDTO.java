package io.hops.hopsworks.api.jobs;

import org.influxdb.dto.QueryResult;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
public class InfluxDBResultDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  private String lastMeasurementTimestamp;
  private QueryResult.Series series;

  public String getLastMeasurementTimestamp() {
    return lastMeasurementTimestamp;
  }

  public void setLastMeasurementTimestamp(String lastMeasurementTimestamp) {
    this.lastMeasurementTimestamp = lastMeasurementTimestamp;
  }

  public QueryResult.Series getSeries() {
    return series;
  }

  public void setSeries(QueryResult.Series series) {
    this.series = series;

    // Get the last measured timestamp
    this.lastMeasurementTimestamp = (String) series.getValues().get(series.getValues().size()-1).get(0);
  }

  public InfluxDBResultDTO() {}


}
