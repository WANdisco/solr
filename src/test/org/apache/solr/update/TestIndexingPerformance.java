/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.update;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.util.AbstractSolrTestCase;

import java.io.IOException;

/** Bypass the normal Solr pipeline and just text indexing performance
 * starting at the update handler.  The same document is indexed repeatedly.
 * 
 * $ ant test -Dtestcase=TestIndexingPerformance -Dargs="-server -Diter=100000"; grep throughput build/test-results/*TestIndexingPerformance.xml
 */
public class TestIndexingPerformance extends AbstractSolrTestCase {

  public String getSchemaFile() { return "schema12.xml"; }
  public String getSolrConfigFile() { return "solrconfig_perf.xml"; }

  public void testIndexingPerf() throws IOException {
    int iter=1000;
    String iterS = System.getProperty("iter");
    if (iterS != null) iter=Integer.parseInt(iterS);

    SolrQueryRequest req = lrf.makeRequest();
    IndexSchema schema = req.getSchema();
    UpdateHandler updateHandler = req.getCore().getUpdateHandler();

    String[] fields = {"text","simple"
            ,"text","test"
            ,"text","how now brown cow"
            ,"text","what's that?"
            ,"text","radical!"
            ,"text","what's all this about, anyway?"
            ,"text","just how fast is this text indexing?"
    };

    Document ldoc = new Document();
    for (int i=0; i<fields.length; i+=2) {
      String field = fields[i];
      String val = fields[i+1];
      Field f = schema.getField(field).createField(val, 1.0f);
      ldoc.add(f);
    }

    AddUpdateCommand add = new AddUpdateCommand();
    add.allowDups = true;
    add.doc = ldoc;

    long start = System.currentTimeMillis();
    for (int i=0; i<iter; i++) {
      updateHandler.addDoc(add);      
    }
    long end = System.currentTimeMillis();
    System.out.println("iter="+iter +" time=" + (end-start) + " throughput=" + ((long)iter*1000)/(end-start));

    //discard all the changes
    updateHandler.rollback(new RollbackUpdateCommand());

    req.close();
  }

}