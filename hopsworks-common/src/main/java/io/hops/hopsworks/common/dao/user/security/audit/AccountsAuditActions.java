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

package io.hops.hopsworks.common.dao.user.security.audit;

public enum AccountsAuditActions {

  // for password change
  PASSWORDCHANGE("PASSWORD CHANGE"),
  // for security question change
  SECQUESTIONCHANGE("SECURITY QUESTION CHANGE"),
  // for profile update
  PROFILEUPDATE("PROFILE UPDATE"),
  // for approving/changing status of users
  USERMANAGEMENT("USER MANAGEMENT"),
  // for mobile lost or yubikeyu lost devices
  LOSTDEVICE("LOST DEVICE REPORT"),
  // for adminchange user account status
  CHANGEDSTATUS("CHANGED STATUS"),
  // to get registration audit logs
  REGISTRATION("REGISTRATION"),
  UNREGISTRATION("UNREGISTRATION"),
  RECOVERY("RECOVERY"),

  QRCODE("QR CODE"),

  SECQUESTION("SECURTY QUESTION RESET"),

  PROFILE("PROFILE UPDATE"),
  PASSWORD("PASSWORD CHANGE"),
  TWO_FACTOR("TWO FACTOR CHANGE"),
  // get all the logs
  ALL("ALL"),

  SUCCESS("SUCCESS"),

  FAILED("FAILED"),

  ABORTED("ABORTED");

  private final String value;

  private AccountsAuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static AccountsAuditActions getAccountsAuditActions(String text) {
    if (text != null) {
      for (AccountsAuditActions b : AccountsAuditActions.values()) {
        if (text.equalsIgnoreCase(b.value)) {
          return b;
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return value;
  }
}
