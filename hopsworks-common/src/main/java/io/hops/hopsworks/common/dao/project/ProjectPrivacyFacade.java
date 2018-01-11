/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.dao.project;

import io.hops.hopsworks.common.dao.user.consent.ConsentStatus;
import java.text.ParseException;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.user.consent.Consents;

import java.util.logging.Logger;

@Stateless
public class ProjectPrivacyFacade {

  private static final Logger logger = Logger.getLogger(
          ProjectPrivacyFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public boolean upload(Consents consent) {
    em.persist(consent);
    return true;
  }

  public Consents getConsentById(int cid) throws ParseException {

    TypedQuery<Consents> q = em.createNamedQuery("Consents.findById",
            Consents.class);
    q.setParameter("id", cid);
    List<Consents> consent = q.getResultList();
    if (consent.size() > 0) {
      return consent.get(0);
    }
    return null;

  }

  public Consents getConsentByName(String name) throws ParseException {

    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByInodePK",
            Consents.class);
    q.setParameter("name", name);
    List<Consents> consent = q.getResultList();
    if (consent.size() > 0) {
      return consent.get(0);
    }
    return null;

  }

  public List<Consents> getAllConsets(int pid) {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByProjectId",
            Consents.class);
    q.setParameter("project.id", pid);

    return q.getResultList();

  }

  public List<Consents> findAllNewConsets(ConsentStatus status) {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findByStatus",
            Consents.class);
    q.setParameter("consentStatus", status);

    return q.getResultList();

  }

  public List<Consents> findAllConsents() {
    TypedQuery<Consents> q = em.createNamedQuery("Consents.findAll",
            Consents.class);
    return q.getResultList();
  }

  public boolean updateConsentStatus(Consents cons, ConsentStatus status) {

    if (cons != null) {
      cons.setConsentStatus(status);
      em.merge(cons);

      return true;
    }
    return false;
  }
}
