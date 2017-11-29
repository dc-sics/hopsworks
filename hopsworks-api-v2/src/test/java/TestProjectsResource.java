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
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.head;
import static io.restassured.RestAssured.post;
import static org.junit.Assert.fail;
//import static io.restassured.RestAssured.when;

public class TestProjectsResource {
  private static String sessionId;
  
  @BeforeClass
  public static void setBaseUri () {
    //RestAssured.authentication = basic("admin@kth.se", "admin");
    RestAssured.baseURI = "http://somemachine/hopsworks-api/api";
    sessionId = given()
        .formParam("email, "")
        .formParam("password", "")
        .when()
        .post("/auth/login")
        .then()
        .statusCode(200)
        .extract()
        .path("sessionID");
  
  }
  @Test
  public void projects_resource_returns_200_with_expected_list(){
    
    given().cookie("SESSION="+sessionId).get("/project").then().statusCode(200);
    
  }
}
