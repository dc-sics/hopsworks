/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.api.zeppelin.rest.message;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * NewNotebookRequest rest api request message
 * <p>
 */
@XmlRootElement
public class NewNotebookRequest {

  String name;
  String defaultInterpreterId;
  List<NewParagraphRequest> paragraphs;

  public NewNotebookRequest() {

  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getDefaultInterpreterId() {
    return defaultInterpreterId;
  }

  public void setDefaultInterpreterId(String defaultInterpreterId) {
    this.defaultInterpreterId = defaultInterpreterId;
  }

  public List<NewParagraphRequest> getParagraphs() {
    return paragraphs;
  }

  @Override
  public String toString() {
    return "NewNotebookRequest{" + "name=" + name + ", defaultInterpreterId=" + defaultInterpreterId + '}';
  }

}
