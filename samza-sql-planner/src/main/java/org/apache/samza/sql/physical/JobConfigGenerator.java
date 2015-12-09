/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.samza.sql.physical;

import org.apache.samza.sql.QueryMetadataStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class JobConfigGenerator {

  public static final String KAFKA_SYSTEM_FACTORY =
      "org.apache.samza.system.kafka.KafkaSystemFactory";
  public static final String SAMZA_KEY_VALUE_STORE_FACTORY =
      "org.apache.samza.storage.kv.KeyValueStorageEngineFactory";
  public static final String YARN_JOB_FACTORY =
      "org.apache.samza.job.yarn.YarnJobFactory";
  public static final String KAFKA_CHECKPOINT_FACTORY =
      "org.apache.samza.checkpoint.kafka.KafkaCheckpointManagerFactory";

  public static final String AVRO_SERDER_FACTORY =
      "org.apache.samza.sql.data.serializers.SqlAvroSerdeFactory";

  private final Map<String, String> jobConfig = new HashMap<String, String>();

  private final QueryMetadataStore queryMetadataStore;

  private final String queryId;

  private String outputStream;


  public JobConfigGenerator(String queryId, QueryMetadataStore queryMetadataStore) {
    this.queryId = queryId;
    this.queryMetadataStore = queryMetadataStore;
  }

  public String registerSchema(String schema) throws Exception {
    return this.queryMetadataStore.registerSchema(queryId, schema);
  }

  public void addInput(String input) {
    if(jobConfig.containsKey(JobConfigurations.TASK_INPUTS)) {
      input = jobConfig.get(JobConfigurations.TASK_INPUTS) + "," + input;
    }

    jobConfig.put(JobConfigurations.TASK_INPUTS, input);
  }

  public void addMetricReporter(String reporter) {
    if(jobConfig.containsKey(JobConfigurations.METRICS_REPOSTERS)) {
      reporter = jobConfig.get(JobConfigurations.METRICS_REPOSTERS) + "," + reporter;
    }

    jobConfig.put(JobConfigurations.METRICS_REPOSTERS, reporter);
  }

  public void addKafkaSystem(String systemName, String zkConnect, String brokerList) throws Exception {

    if(jobConfig.containsKey(String.format(JobConfigurations.SYSTEMS_SAMZA_FACTORY, systemName))) {
      throw new Exception(String.format("System with name %s already exists in the configuraiton.",
          systemName));
    }

    jobConfig.put(
        String.format(JobConfigurations.SYSTEMS_SAMZA_FACTORY, systemName),
        KAFKA_SYSTEM_FACTORY);
    jobConfig.put(
        String.format(JobConfigurations.SYSTEMS_CONSUMER_ZOOKEEPER_CONNECT, systemName),
        zkConnect);
    jobConfig.put(
        String.format(JobConfigurations.SYSTEMS_PRODUCER_METADATA_BROKER_LIST, systemName),
        brokerList);
  }

  public void addSerde(String serdeName, String serdeClass) {
    jobConfig.put(
        String.format(JobConfigurations.SERIALIZERS_REGISTRY_CLASS, serdeName),
        serdeClass
    );
  }

  public void addStore(String storeName, String keySerde, String msgSerde, String changelogStream) throws Exception {
    isValidSerde(keySerde);
    isValidSerde(msgSerde);

    if(jobConfig.containsKey(String.format(JobConfigurations.STORES_FACTORY, storeName))) {
      throw new Exception(String.format("Store with name %s already exists.", storeName));
    }

    jobConfig.put(
        String.format(JobConfigurations.STORES_FACTORY, storeName),
        SAMZA_KEY_VALUE_STORE_FACTORY);
    jobConfig.put(
        String.format(JobConfigurations.STORES_KEY_SERDE, storeName),
        keySerde);
    jobConfig.put(
        String.format(JobConfigurations.STORES_MSG_SERDE, storeName),
        msgSerde);
    jobConfig.put(
        String.format(JobConfigurations.STORES_CHANGELOG, storeName),
        changelogStream);
  }

  public boolean isSerdeExists(String serdeName) {
    return jobConfig.containsKey(
        String.format(JobConfigurations.SERIALIZERS_REGISTRY_CLASS, serdeName));
  }

  public void addStream(String system, String streamName,
                        String keySerde, String msgSerde, boolean isBootstrap) throws Exception {
    isValidSerde(keySerde);
    isValidSerde(msgSerde);
    isValidSystem(system);

    jobConfig.put(
        String.format(JobConfigurations.SYSTEMS_STREAMS_SAMZA_KEY_SERDE, system, streamName),
        keySerde);
    jobConfig.put(
        String.format(JobConfigurations.SYSTEMS_STREAMS_SAMZA_MSG_SERDE, system, streamName),
        msgSerde);
    jobConfig.put(
        String.format(JobConfigurations.SYSTEMS_STREAMS_SAMZA_BOOTSTRAP, system, streamName),
        String.valueOf(isBootstrap));
    jobConfig.put(
        String.format(JobConfigurations.SYSTEMS_STREAMS_SAMZA_OFFSET_DEFAULT, system, streamName),
        "upcoming");
  }

  private void isValidSystem(String systemName) throws Exception {
    if(!isSystemExists(systemName))
      throw new Exception(String.format("Cannot find system %s.", systemName));
  }

  private void isValidSerde(String serde) throws Exception {
    if(!isSerdeExists(serde))
      throw new Exception(String.format("Cannot find serde %s. Please register serde first.",
          serde));
  }

  public boolean isSystemExists(String systemName) {
    return jobConfig.containsKey(String.format(JobConfigurations.SYSTEMS_SAMZA_FACTORY, systemName));
  }

  public void setJobFactory(String jobFactory) {
    jobConfig.put(JobConfigurations.JOB_FACTORY_CLASS, jobFactory);
  }

  public void setJobName(String jobName) {
    jobConfig.put(JobConfigurations.JOB_NAME, jobName);
  }

  public void setCheckpointSystem(String systemName) throws Exception {
    isValidSystem(systemName);
    jobConfig.put(JobConfigurations.TASK_CHECKPOINT_SYSTEM, systemName);
  }

  public void setTaskCheckpointFactory(String checkpointFactory) {
    jobConfig.put(JobConfigurations.TASK_CHECKPOINT_FACTORY, checkpointFactory);
  }


  public void addConfig(String key, String value) {
    jobConfig.put(key, value);
  }

  public Map<String, String> getJobConfig(){
    return jobConfig;
  }

  public void setOutputStream(String outputStream) {
    this.outputStream = outputStream;
  }

  public String getOutputStream(){
    return outputStream;
  }

  public String registerMessageSchema(String msgSchema) throws Exception {
    return queryMetadataStore.registerSchema(queryId, msgSchema);
  }
}
