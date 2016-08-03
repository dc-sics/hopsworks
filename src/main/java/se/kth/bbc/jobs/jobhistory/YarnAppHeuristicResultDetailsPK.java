/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author vasileios
 */
@Embeddable
public class YarnAppHeuristicResultDetailsPK implements Serializable {
    @Basic(optional = false)
    @NotNull
    @Column(name = "yarn_app_heuristic_result_id")
    private int yarnAppHeuristicResultId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 128)
    @Column(name = "name")
    private String name;

    public YarnAppHeuristicResultDetailsPK() {
    }

    public YarnAppHeuristicResultDetailsPK(int yarnAppHeuristicResultId, String name) {
        this.yarnAppHeuristicResultId = yarnAppHeuristicResultId;
        this.name = name;
    }

    public int getYarnAppHeuristicResultId() {
        return yarnAppHeuristicResultId;
    }

    public void setYarnAppHeuristicResultId(int yarnAppHeuristicResultId) {
        this.yarnAppHeuristicResultId = yarnAppHeuristicResultId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) yarnAppHeuristicResultId;
        hash += (name != null ? name.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof YarnAppHeuristicResultDetailsPK)) {
            return false;
        }
        YarnAppHeuristicResultDetailsPK other = (YarnAppHeuristicResultDetailsPK) object;
        if (this.yarnAppHeuristicResultId != other.yarnAppHeuristicResultId) {
            return false;
        }
        if ((this.name == null && other.name != null) || (this.name != null && !this.name.equals(other.name))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "se.kth.bbc.jobs.jobhistory.YarnAppHeuristicResultDetailsPK[ yarnAppHeuristicResultId=" + yarnAppHeuristicResultId + ", name=" + name + " ]";
    }
    
}
