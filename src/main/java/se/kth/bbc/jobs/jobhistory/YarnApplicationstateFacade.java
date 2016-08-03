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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kth.bbc.jobs.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class YarnApplicationstateFacade extends AbstractFacade<YarnApplicationstate> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnApplicationstateFacade() {
    super(YarnApplicationstate.class);
  }

  @Override
  public List<YarnApplicationstate> findAll() {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findAll",
            YarnApplicationstate.class);
    return query.getResultList();
  }

  public List<YarnApplicationstate> findByAppname(String appname) {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findByAppname",
            YarnApplicationstate.class).setParameter(
                    "appname", appname);
    return query.getResultList();
  }

  public List<YarnApplicationstate> findByAppuserAndAppState(String appUser,
          String appState) {
    TypedQuery<YarnApplicationstate> query = em.createNamedQuery(
            "YarnApplicationstate.findByAppuserAndAppsmstate",
            YarnApplicationstate.class).setParameter("appuser", appUser).
            setParameter("appsmstate", appState);
    return query.getResultList();
  }

  public YarnApplicationstate findByAppId(String appId) {
    try {
      return em.createNamedQuery("YarnApplicationstate.findByApplicationid",
              YarnApplicationstate.class).setParameter(
                      "applicationid", appId).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }

  }
}
