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

package io.hops.hopsworks.api.kibana;

/**
 * Enumeration that defines the filtered URIs of Kibana going through
 * HopsWorks proxy servlet.
 * <p>
 * <p>
 * List of requests (prefix is /hopsworks-api/kibana)
 * 1.
 * /elasticsearch/_mget?timeout=0&ignore_unavailable=true&preference=1484560870227
 * (filter request params)
 * 2.
 * /elasticsearch/.kibana/index-pattern/_search?fields=
 * (filter json response)
 * 3.
 * /elasticsearch/.kibana/_mapping/*
 * /field/_source?_=1484560870948 (do nothing)
 * 4.
 * /elasticsearch/_msearch?timeout=0&ignore_unavailable=true&preference=1484560870227
 * (filter request payload)
 * 5.
 * /elasticsearch/.kibana/index-pattern/demo_admin000
 * (filter uri)
 * 6.
 * /elasticsearch/logstash-*
 * /_mapping/field/*?_=1484561606214&ignore_unavailable
 * =false&allow_no_indices=false&include_defaults=true
 * (filter index in URI)
 * 7.
 * /elasticsearch/.kibana/search/_search?size=100
 * (filter saved searches in response, should be prefixed with projectId)
 * 8.
 * /elasticsearch/.kibana/visualization/_search?size=100
 * (similar 7)
 * 9.
 * /elasticsearch/.kibana/dashboard/_search?size=100
 * (similar to 7)
 * <p>
 * <p>
 */
public enum KibanaFilter {
  KIBANA_SAVED_OBJECTS_API,
  ELASTICSEARCH_SEARCH//elasticsearch/*/_search
}