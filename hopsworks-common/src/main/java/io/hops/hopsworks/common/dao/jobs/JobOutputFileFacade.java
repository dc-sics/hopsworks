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

package io.hops.hopsworks.common.dao.jobs;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobhistory.Execution;

@Stateless
public class JobOutputFileFacade extends AbstractFacade<JobOutputFile> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public JobOutputFileFacade() {
    super(JobOutputFile.class);
  }

  public void create(Execution execution, String name, String path) {
    if (execution == null) {
      throw new NullPointerException(
              "Cannot create an OutputFile for null Execution.");
    }
    JobOutputFile file = new JobOutputFile(execution.getId(), name);
    file.setPath(path);
    em.persist(file);
  }

  public List<JobOutputFile> findOutputFilesForExecutionId(Long id) {
    TypedQuery<JobOutputFile> q = em.createNamedQuery(
            "JobOutputFile.findByExecutionId", JobOutputFile.class);
    q.setParameter("jobId", id);
    return q.getResultList();
  }

  public JobOutputFile findByNameAndExecutionId(String name, Long id) {
    TypedQuery<JobOutputFile> q = em.createNamedQuery(
            "JobOutputFile.findByNameAndExecutionId", JobOutputFile.class);
    q.setParameter("name", name);
    q.setParameter("jobId", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException | NonUniqueResultException e) {
      return null;
    }
  }

}
