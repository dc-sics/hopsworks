/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.hops.hopsworks.common.dao.jobs.quota;

import io.hops.hopsworks.common.dao.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class YarnProjectsQuotaFacade extends
        AbstractFacade<YarnProjectsQuota> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public void persistYarnProjectsQuota(YarnProjectsQuota yarnProjectsQuota) {
    em.persist(yarnProjectsQuota);
  }

  public YarnProjectsQuotaFacade() {
    super(YarnProjectsQuota.class);
  }

  public YarnProjectsQuota findByProjectName(String projectname) {
    TypedQuery<YarnProjectsQuota> query = em.
            createNamedQuery("YarnProjectsQuota.findByProjectname",
                    YarnProjectsQuota.class).setParameter("projectname",
                    projectname);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void flushEm() {
    em.flush();
  }

  @Override
  public List<YarnProjectsQuota> findAll() {
    TypedQuery<YarnProjectsQuota> query = em.createNamedQuery(
            "YarnProjectsQuota.findAll",
            YarnProjectsQuota.class);
    return query.getResultList();
  }

  public void changeYarnQuota(String projectname, float quota) {
    YarnProjectsQuota project = findByProjectName(projectname);
    if (project != null) {
      project.setQuotaRemaining(quota);
      em.merge(project);
    }
  }

  public YarnPriceMultiplicator getMultiplicator() {
    try {
      TypedQuery<YarnPriceMultiplicator> query = em.
              createNamedQuery("YarnPriceMultiplicator.findAll",
                      YarnPriceMultiplicator.class).setMaxResults(1);
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }

  }

}
