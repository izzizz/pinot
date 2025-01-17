/**
 * Copyright (C) 2014-2015 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.pinot.tools.admin.command;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.args4j.Option;


/**
 * Super class for all the commands.
 * Implements the common printUsage method.
 *
 */
public class AbstractBaseCommand {
  static final String DEFAULT_ZK_ADDRESS = "localhost:2181";
  static final String DEFAULT_CONTROLLER_PORT = "9000";
  static final String DEFAULT_CLUSTER_NAME = "PinotCluster";
  static final String URI_TABLES_PATH = "/tables/";

  static final String TMP_DIR = System.getProperty("java.io.tmpdir") + File.separator;

  public AbstractBaseCommand(boolean addShutdownHook) {
    if (addShutdownHook) {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          cleanup();
        }
      });
    }
  }

  public AbstractBaseCommand() {
    this(true);
  }

  public void cleanup() {
  };

  public String getName() {
    return "BaseCommand";
  }

  public void printUsage() {
    System.out.println("Usage: " + this.getName());

    for (Field f : this.getClass().getDeclaredFields()) {
      if (f.isAnnotationPresent(Option.class)) {
        Option option = f.getAnnotation(Option.class);

        System.out.println(String.format("\t%-25s %-30s: %s (required=%s)", option.name(), option.metaVar(),
            option.usage(), option.required()));
      }
    }
  }

  public static int getPID() {
    String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
    return Integer.parseInt(processName.split("@")[0]);
  }

  protected void savePID(String fileName) throws IOException {
    FileWriter pidFile = new FileWriter(fileName);
    pidFile.write(Integer.toString(getPID()));
    pidFile.close();
  }

  public static String sendPostRequest(String urlString, String payload) throws UnsupportedEncodingException,
      IOException, JSONException {
    final URL url = new URL(urlString);
    final URLConnection conn = url.openConnection();

    conn.setDoOutput(true);
    final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));

    writer.write(payload, 0, payload.length());
    writer.flush();

    final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
    final StringBuilder sb = new StringBuilder();
    String line = null;

    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }

    return sb.toString();
  }

  public static JSONObject postQuery(String query, String brokerBaseApiUrl) throws Exception {
    final JSONObject json = new JSONObject();
    json.put("pql", query);

    final long start = System.currentTimeMillis();
    final URLConnection conn = new URL(brokerBaseApiUrl + "/query").openConnection();
    conn.setDoOutput(true);
    final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
    final String reqStr = json.toString();
    System.out.println("reqStr = " + reqStr);

    writer.write(reqStr, 0, reqStr.length());
    writer.flush();
    final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

    final StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }

    final long stop = System.currentTimeMillis();

    final String res = sb.toString();
    System.out.println("res = " + res);
    final JSONObject ret = new JSONObject(res);
    ret.put("totalTime", (stop - start));

    return ret;
  }

  PropertiesConfiguration readConfigFromFile(String configFileName) throws ConfigurationException {
    if (configFileName != null) {
      File configFile = new File(configFileName);

      if (configFile.exists()) {
        return new PropertiesConfiguration(configFile);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
