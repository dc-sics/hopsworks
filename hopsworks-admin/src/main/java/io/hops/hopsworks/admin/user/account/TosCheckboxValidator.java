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

package io.hops.hopsworks.admin.user.account;

import io.hops.hopsworks.common.constants.auth.AccountStatusErrorMessages;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@ManagedBean
@RequestScoped
public class TosCheckboxValidator implements Validator {

  public boolean check = false;

  public boolean isCheck() {
    return check;
  }

  public void setCheck(boolean check) {
    this.check = check;
  }

  @Override
  public void validate(FacesContext context, UIComponent component,
          Object value) throws ValidatorException {

    String cb = value.toString();

    if (!cb.equals("true")) {
      FacesMessage facesMsg = new FacesMessage(
              AccountStatusErrorMessages.TOS_ERROR);
      facesMsg.setSeverity(FacesMessage.SEVERITY_ERROR);
      throw new ValidatorException(facesMsg);
    }
  }

}
