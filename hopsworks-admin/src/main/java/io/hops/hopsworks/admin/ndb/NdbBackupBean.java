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
package io.hops.hopsworks.admin.ndb;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.ndb.NdbBackup;
import io.hops.hopsworks.common.dao.ndb.NdbBackupFacade;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.event.RowEditEvent;

@ManagedBean(name = "ndbBackupBean")
@SessionScoped
public class NdbBackupBean implements Serializable {

  private static final Logger logger = Logger.getLogger(
          NdbBackupBean.class.getName());

  @EJB
  private NdbBackupFacade ndbBackupFacade;
  @EJB
  private Settings settings;

  public List<NdbBackup> allBackups = new ArrayList<>();

  public void setAllBackups(List<NdbBackup> allBackups) {
    this.allBackups = allBackups;
  }

  public List<NdbBackup> getAllBackups() {
    List<NdbBackup> all = ndbBackupFacade.findAll();
    if (all != null) {
      allBackups = all;
    }
    return allBackups;
  }

  public void onRowEdit(RowEditEvent event)
          throws IOException {

  }

  public void onRowCancel(RowEditEvent event) {
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String restore(Integer backupId) {

    String prog = settings.getHopsworksDomainDir() + "/bin/ndb_backup.sh";
    int exitValue;

    String[] command = {"/usr/bin/sudo", prog, "restore", backupId.toString()};
    ProcessBuilder pb = new ProcessBuilder(command);

    try {
      Process p = pb.start();
      p.waitFor();
      exitValue = p.exitValue();
    } catch (IOException | InterruptedException ex) {

      logger.log(Level.SEVERE, "Problem restoring a backup: {0}", ex.toString());
      return "RESTORE_FAILED";
    }
    return exitValue == 0 ? "RESTORE_OK" : "RESTORE_FAILED";
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String remove(Integer backupId) {

    String prog = settings.getHopsworksDomainDir() + "/bin/ndb_backup.sh";
    int exitValue;

    String[] command = {"/usr/bin/sudo", prog, "remove", backupId.toString()};
    ProcessBuilder pb = new ProcessBuilder(command);

    try {
      Process p = pb.start();
      p.waitFor();
      exitValue = p.exitValue();
      if (exitValue == 0) {
        ndbBackupFacade.removeBackup(backupId);        
      }
    } catch (IOException | InterruptedException ex) {

      logger.log(Level.SEVERE, "Problem removing the backup: {0}", ex.toString());
      return "RESTORE_FAILED";
    }
    return exitValue == 0 ? "RESTORE_OK" : "RESTORE_FAILED";
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String startBackup() {
    String prog = settings.getHopsworksDomainDir() + "/bin/ndb_backup.sh";
    int exitValue;
    Integer id = 1;
    String[] command = {"/usr/bin/sudo", prog, "backup"};
    ProcessBuilder pb = new ProcessBuilder(command);
    try {
      Process process = pb.start();

      BufferedReader br = new BufferedReader(new InputStreamReader(
              process.getInputStream(), Charset.forName("UTF8")));
      String line;
      String pattern = "(.*)Backup (\\d+)(.*)";
      Pattern r = Pattern.compile(pattern);
      boolean foundId = false;
      while ((line = br.readLine()) != null) {
        logger.info(line);
        Matcher m = r.matcher(line);
        if (m.find()) {
          id = Integer.parseInt(m.group(2));
          foundId = true;
        }
      }

      process.waitFor();
      exitValue = process.exitValue();
      if (exitValue == 0 && foundId) {
        NdbBackup newBackup = new NdbBackup(id);
        ndbBackupFacade.persistBackup(newBackup);
      }
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Problem starting a backup: {0}", ex.
              toString());
      exitValue = -2;
    }
    if (exitValue == 0) {
      return "BACKUP_OK";
    } else {
      MessagesController.addSecurityErrorMessage("Backup failed: " + exitValue);
      return "BACKUP_FAILED";
    }
  }
}
