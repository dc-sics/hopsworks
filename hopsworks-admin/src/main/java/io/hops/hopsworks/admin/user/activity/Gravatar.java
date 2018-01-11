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

package io.hops.hopsworks.admin.user.activity;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Custom class to get the gravatar image from an email. Implemented because of
 * issues with the jgravatar library.
 */
public class Gravatar {

  private static final String BASIC_URL = "http://www.gravatar.com/avatar/";
  private static final String DEFAULT = "?d=mm";

  public static String hex(byte[] array) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < array.length; ++i) {
      sb.append(Integer.toHexString((array[i]
              & 0xFF) | 0x100).substring(1, 3));
    }
    return sb.toString();
  }

  public static String md5Hex(String message) {
    try {
      MessageDigest md
              = MessageDigest.getInstance("MD5");
      return hex(md.digest(message.getBytes("CP1252")));
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
    }
    return null;
  }

  public static String getUrl(String email) {
    String hash = md5Hex(email);
    return BASIC_URL + hash + DEFAULT;
  }

  public static String getUrl(String email, int size) {
    String url = getUrl(email);
    return url + "&s=" + size;
  }

}
