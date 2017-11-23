/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.apiV2;

import io.hops.hopsworks.common.exception.AppException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class Util {
  
  public static void except(Response.Status status, String msg)
      throws AppException {
    throw new AppException(status.getStatusCode(), msg);
  }
  
  public static Response noContent(){
    return Response.noContent().build();
  }
  public static Response ok(Object entity){
    return Response.ok(entity).type(MediaType.APPLICATION_JSON_TYPE).build();
  }
}
