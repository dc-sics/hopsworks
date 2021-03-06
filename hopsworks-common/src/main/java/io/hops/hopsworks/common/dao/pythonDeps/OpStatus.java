/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.dao.pythonDeps;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OpStatus {

  private String channelUrl = "default";
  private String installType;
  private String machineType;
  private String lib;
  private String version;
  private String op;
  private String status = "Not Installed";
  private List<HostOpStatus> hosts = new ArrayList<>();

  public OpStatus() {
  }

  public OpStatus(String op, String installType, String machineType, String channelUrl, String lib, String version) {
    this.op = op;
    this.installType = installType;
    this.machineType = machineType;
    this.channelUrl = channelUrl;
    this.lib = lib;
    this.version = version;
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public List<HostOpStatus> getHosts() {
    return hosts;
  }

  public String getStatus() {
    return status;
  }

  public void setHosts(List<HostOpStatus> hosts) {
    this.hosts = hosts;
  }

  public void addHost(HostOpStatus host) {
    this.hosts.add(host);
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getChannelUrl() {
    return channelUrl;
  }

  public void setChannelUrl(String channelUrl) {
    this.channelUrl = channelUrl;
  }

  public String getInstallType() {
    return installType;
  }

  public void setInstallType(String installType) {
    this.installType = installType;
  }

  public String getMachineType() {
    return machineType;
  }

  public void setMachineType(String machineType) {
    this.machineType = machineType;
  }

  public String getLib() {
    return lib;
  }

  public void setLib(String lib) {
    this.lib = lib;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("[");
    sb.append(channelUrl)
        .append(",").append(lib)
        .append(",").append(version)
        .append(",").append(op)
        .append(",").append(status)
        .append(",(");
    hosts.forEach((h) -> {
      sb.append(h.toString());
    });
    sb.append(")]");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof OpStatus) {
      OpStatus pd = (OpStatus) o;
      if (pd.getChannelUrl().compareToIgnoreCase(this.channelUrl) == 0
          && pd.getLib().compareToIgnoreCase(this.lib) == 0
          && pd.getVersion().compareTo(this.version) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (this.channelUrl.hashCode() / 3 + this.lib.hashCode()
        + this.version.hashCode()) / 2;
  }
}
