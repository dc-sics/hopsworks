package io.hops.hopsworks.common.dao.jobs.description;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TfUrlsDTO {

  String name;
  String tbUrl;

  public TfUrlsDTO() {
  }

  public TfUrlsDTO(String name, String tbUrl) {
    this.name = name;
    this.tbUrl = tbUrl;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTbUrl() {
    return tbUrl;
  }

  public void setTbUrl(String tbUrl) {
    this.tbUrl = tbUrl;
  }

}
