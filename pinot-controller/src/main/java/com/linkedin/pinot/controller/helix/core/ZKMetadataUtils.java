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
package com.linkedin.pinot.controller.helix.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.joda.time.Duration;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.linkedin.pinot.common.data.Schema;
import com.linkedin.pinot.common.metadata.resource.OfflineDataResourceZKMetadata;
import com.linkedin.pinot.common.metadata.resource.RealtimeDataResourceZKMetadata;
import com.linkedin.pinot.common.metadata.segment.OfflineSegmentZKMetadata;
import com.linkedin.pinot.common.metadata.stream.KafkaStreamMetadata;
import com.linkedin.pinot.common.segment.SegmentMetadata;
import com.linkedin.pinot.common.utils.CommonConstants;
import com.linkedin.pinot.common.utils.CommonConstants.Helix;
import com.linkedin.pinot.common.utils.CommonConstants.Helix.DataSource.Realtime.StreamType;
import com.linkedin.pinot.common.utils.CommonConstants.Segment.SegmentType;
import com.linkedin.pinot.common.utils.StringUtil;
import com.linkedin.pinot.controller.api.pojos.DataResource;


public class ZKMetadataUtils {
  private static Logger LOGGER = Logger.getLogger(ZKMetadataUtils.class);

  public static OfflineDataResourceZKMetadata getOfflineDataResourceMetadata(DataResource resource) {
    OfflineDataResourceZKMetadata offlineDataResourceMetadata = new OfflineDataResourceZKMetadata();
    offlineDataResourceMetadata.setResourceName(resource.getResourceName());
    offlineDataResourceMetadata.addToTableList(resource.getTableName());
    offlineDataResourceMetadata.setTimeColumnName(resource.getTimeColumnName());
    offlineDataResourceMetadata.setTimeType(resource.getTimeType());
    offlineDataResourceMetadata.setNumDataInstances(resource.getNumberOfDataInstances());
    offlineDataResourceMetadata.setNumDataReplicas(resource.getNumberOfCopies());
    offlineDataResourceMetadata.setNumBrokerInstance(resource.getNumberOfBrokerInstances());
    offlineDataResourceMetadata.setBrokerTag(resource.getBrokerTagName());
    offlineDataResourceMetadata.setPushFrequency(resource.getPushFrequency());
    offlineDataResourceMetadata.setSegmentAssignmentStrategy(resource.getSegmentAssignmentStrategy());
    Map<String, String> metadataMap = new HashMap<String, String>();
    if (resource.getMetadata() != null) {
      Iterator<String> fieldNameIter = resource.getMetadata().fieldNames();
      while (fieldNameIter.hasNext()) {
        String fieldName = fieldNameIter.next();
        metadataMap.put(StringUtil.join(".", CommonConstants.Helix.DataSource.METADATA, fieldName),
            resource.getMetadata().get(fieldName).textValue());
      }
    }
    offlineDataResourceMetadata.setMetadata(metadataMap);
    try {
      offlineDataResourceMetadata.setRetentionTimeUnit(TimeUnit.valueOf(resource.getRetentionTimeUnit()));
      offlineDataResourceMetadata.setRetentionTimeValue(Integer.parseInt(resource.getRetentionTimeValue()));
    } catch (Exception e) {
      LOGGER.warn("No retention config for - " + resource);
    }

    return offlineDataResourceMetadata;
  }

  public static RealtimeDataResourceZKMetadata getRealtimeDataResourceMetadata(DataResource resource) {
    RealtimeDataResourceZKMetadata realtimeDataResourceMetadata = new RealtimeDataResourceZKMetadata();
    realtimeDataResourceMetadata.setResourceName(resource.getResourceName());
    realtimeDataResourceMetadata.addToTableList(resource.getTableName());
    realtimeDataResourceMetadata.setTimeColumnName(resource.getTimeColumnName());
    realtimeDataResourceMetadata.setTimeType(resource.getTimeType());
    realtimeDataResourceMetadata.setNumDataInstances(resource.getNumberOfDataInstances());
    realtimeDataResourceMetadata.setNumDataReplicas(resource.getNumberOfCopies());
    realtimeDataResourceMetadata.setNumBrokerInstance(resource.getNumberOfBrokerInstances());
    realtimeDataResourceMetadata.setBrokerTag(resource.getBrokerTagName());
    realtimeDataResourceMetadata.setStreamType(extractStreamTypeFromDataResource(resource));

    Map<String, String> metadataMap = new HashMap<String, String>();
    Map<String, String> schemaMap = new HashMap<String, String>();
    Map<String, String> streamMap = new HashMap<String, String>();
    Iterator<String> fieldNameIter = resource.getMetadata().fieldNames();
    while (fieldNameIter.hasNext()) {
      String fieldName = fieldNameIter.next();
      if (fieldName.startsWith(CommonConstants.Helix.DataSource.SCHEMA + ".")) {
        schemaMap.put(fieldName, resource.getMetadata().get(fieldName).textValue());
        continue;
      }
      if (fieldName.startsWith(CommonConstants.Helix.DataSource.STREAM + ".")) {
        streamMap.put(fieldName, resource.getMetadata().get(fieldName).textValue());
        continue;
      }
      metadataMap.put(StringUtil.join(".", CommonConstants.Helix.DataSource.METADATA, fieldName),
          resource.getMetadata().get(fieldName).textValue());
    }
    realtimeDataResourceMetadata.setMetadata(metadataMap);
    realtimeDataResourceMetadata.setDataSchema(Schema.getSchemaFromMap(schemaMap));
    switch (realtimeDataResourceMetadata.getStreamType()) {
      case kafka:
        realtimeDataResourceMetadata.setStreamMetadata(new KafkaStreamMetadata(streamMap));
        break;

      default:
        break;
    }

    try {
      realtimeDataResourceMetadata.setRetentionTimeUnit(TimeUnit.valueOf(resource.getRetentionTimeUnit()));
      realtimeDataResourceMetadata.setRetentionTimeValue(Integer.parseInt(resource.getRetentionTimeValue()));
    } catch (Exception e) {
      LOGGER.warn("No retention config for - " + resource);
    }

    return realtimeDataResourceMetadata;
  }

  private static StreamType extractStreamTypeFromDataResource(DataResource resource) {
    ObjectNode metadata = resource.getMetadata();
    String streamType = metadata.get(Helix.DataSource.Realtime.STREAM_TYPE).textValue();
    if (streamType == null || streamType.isEmpty()) {
      return null;
    }
    return StreamType.valueOf(streamType);
  }

  public static OfflineSegmentZKMetadata updateSegmentMetadata(OfflineSegmentZKMetadata offlineSegmentZKMetadata, SegmentMetadata segmentMetadata) {
    offlineSegmentZKMetadata.setSegmentName(segmentMetadata.getName());
    offlineSegmentZKMetadata.setResourceName(segmentMetadata.getResourceName());
    offlineSegmentZKMetadata.setTableName(segmentMetadata.getTableName());
    offlineSegmentZKMetadata.setIndexVersion(segmentMetadata.getVersion());
    offlineSegmentZKMetadata.setSegmentType(SegmentType.OFFLINE);

    offlineSegmentZKMetadata.setTimeUnit(extractTimeUnitFromDuration(segmentMetadata.getTimeGranularity()));
    if (segmentMetadata.getTimeInterval() == null) {
      offlineSegmentZKMetadata.setStartTime(-1);
      offlineSegmentZKMetadata.setEndTime(-1);
    } else {
      offlineSegmentZKMetadata.setStartTime(
          offlineSegmentZKMetadata.getTimeUnit().convert(segmentMetadata.getTimeInterval().getStartMillis(), TimeUnit.MILLISECONDS));
      offlineSegmentZKMetadata.setEndTime(
          offlineSegmentZKMetadata.getTimeUnit().convert(segmentMetadata.getTimeInterval().getEndMillis(), TimeUnit.MILLISECONDS));
    }
    offlineSegmentZKMetadata.setTotalDocs(segmentMetadata.getTotalDocs());
    offlineSegmentZKMetadata.setCreationTime(segmentMetadata.getIndexCreationTime());
    offlineSegmentZKMetadata.setCrc(Long.parseLong(segmentMetadata.getCrc()));
    return offlineSegmentZKMetadata;
  }

  private static TimeUnit extractTimeUnitFromDuration(Duration timeGranularity) {
    if (timeGranularity == null) {
      return null;
    }
    long timeUnitInMills = timeGranularity.getMillis();
    for (TimeUnit timeUnit : TimeUnit.values()) {
      if (timeUnit.toMillis(1) == timeUnitInMills) {
        return timeUnit;
      }
    }
    return null;
  }

  public static OfflineDataResourceZKMetadata updateOfflineZKMetadataByDataResource(OfflineDataResourceZKMetadata offlineDataResourceZKMetadata, DataResource resource) {
    offlineDataResourceZKMetadata.setTimeColumnName(resource.getTimeColumnName());
    offlineDataResourceZKMetadata.setTimeType(resource.getTimeType());
    offlineDataResourceZKMetadata.setPushFrequency(resource.getPushFrequency());
    offlineDataResourceZKMetadata.setSegmentAssignmentStrategy(resource.getSegmentAssignmentStrategy());

    Map<String, String> metadataMap = new HashMap<String, String>();
    Iterator<String> fieldNameIter = resource.getMetadata().fieldNames();
    while (fieldNameIter.hasNext()) {
      String fieldName = fieldNameIter.next();
      metadataMap.put(StringUtil.join(".", CommonConstants.Helix.DataSource.METADATA, fieldName),
          resource.getMetadata().get(fieldName).textValue());
    }
    offlineDataResourceZKMetadata.setMetadata(metadataMap);
    try {
      offlineDataResourceZKMetadata.setRetentionTimeUnit(TimeUnit.valueOf(resource.getRetentionTimeUnit()));
      offlineDataResourceZKMetadata.setRetentionTimeValue(Integer.parseInt(resource.getRetentionTimeValue()));
    } catch (Exception e) {
      LOGGER.warn("No retention config for - " + resource);
    }

    return offlineDataResourceZKMetadata;

  }

  public static RealtimeDataResourceZKMetadata updateRealtimeZKMetadataByDataResource(RealtimeDataResourceZKMetadata realtimeDataResourceZKMetadata, DataResource resource) {
    realtimeDataResourceZKMetadata.setTimeColumnName(resource.getTimeColumnName());
    realtimeDataResourceZKMetadata.setTimeType(resource.getTimeType());
    realtimeDataResourceZKMetadata.setTimeType(resource.getTimeType());
    realtimeDataResourceZKMetadata.setStreamType(extractStreamTypeFromDataResource(resource));

    Map<String, String> metadataMap = new HashMap<String, String>();
    Map<String, String> schemaMap = new HashMap<String, String>();
    Map<String, String> streamMap = new HashMap<String, String>();
    Iterator<String> fieldNameIter = resource.getMetadata().fieldNames();
    while (fieldNameIter.hasNext()) {
      String fieldName = fieldNameIter.next();
      if (fieldName.startsWith(CommonConstants.Helix.DataSource.SCHEMA + ".")) {
        schemaMap.put(fieldName, resource.getMetadata().get(fieldName).textValue());
        continue;
      }
      if (fieldName.startsWith(CommonConstants.Helix.DataSource.STREAM + ".")) {
        streamMap.put(fieldName, resource.getMetadata().get(fieldName).textValue());
        continue;
      }
      metadataMap.put(StringUtil.join(".", CommonConstants.Helix.DataSource.METADATA, fieldName),
          resource.getMetadata().get(fieldName).textValue());
    }
    realtimeDataResourceZKMetadata.setMetadata(metadataMap);
    if (schemaMap.size() > 0) {
      realtimeDataResourceZKMetadata.setDataSchema(Schema.getSchemaFromMap(schemaMap));
    }
    if (streamMap.size() > 0) {
      switch (realtimeDataResourceZKMetadata.getStreamType()) {
        case kafka:
          realtimeDataResourceZKMetadata.setStreamMetadata(new KafkaStreamMetadata(streamMap));
          break;
        default:
          break;
      }
    }

    try {
      realtimeDataResourceZKMetadata.setRetentionTimeUnit(TimeUnit.valueOf(resource.getRetentionTimeUnit()));
      realtimeDataResourceZKMetadata.setRetentionTimeValue(Integer.parseInt(resource.getRetentionTimeValue()));
    } catch (Exception e) {
      LOGGER.warn("No retention config for - " + resource);
    }

    return realtimeDataResourceZKMetadata;
  }
}