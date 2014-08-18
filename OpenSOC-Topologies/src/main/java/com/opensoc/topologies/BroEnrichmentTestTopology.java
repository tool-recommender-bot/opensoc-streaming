/*
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

package com.opensoc.topologies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import storm.kafka.bolt.KafkaBolt;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;

import com.opensoc.enrichment.adapters.cif.CIFHbaseAdapter;
import com.opensoc.enrichment.adapters.geo.GeoMysqlAdapter;
import com.opensoc.enrichment.adapters.whois.WhoisHBaseAdapter;
import com.opensoc.enrichment.common.EnrichmentAdapter;
import com.opensoc.enrichment.common.GenericEnrichmentBolt;
import com.opensoc.enrichment.host.HostAdapter;
import com.opensoc.indexing.TelemetryIndexingBolt;
import com.opensoc.indexing.adapters.ESBaseBulkAdapter;
import com.opensoc.json.serialization.JSONKryoSerializer;
import com.opensoc.parsing.AbstractParserBolt;
import com.opensoc.parsing.TelemetryParserBolt;
import com.opensoc.parsing.parsers.BasicBroParser;
import com.opensoc.test.spouts.GenericInternalTestSpout;

/**
 * This is a basic example of a Storm topology.
 */

public class BroEnrichmentTestTopology {

	public static void main(String[] args) throws Exception {

		String config_path = "";

		try {
			config_path = args[0];
		} catch (Exception e) {
			config_path = "TopologyConfigs/bro.conf";
		}

		Configuration config = new PropertiesConfiguration(config_path);

		String topology_name = config.getString("topology.name");

		TopologyBuilder builder = new TopologyBuilder();

		Config conf = new Config();
		conf.registerSerialization(JSONObject.class, JSONKryoSerializer.class);
		conf.setDebug(config.getBoolean("debug.mode"));

		// ------------KAFKA spout configuration

	/*	BrokerHosts zk = new ZkHosts(config.getString("kafka.zk"));
		String input_topic = config.getString("spout.kafka.topic");
		SpoutConfig kafkaConfig = new SpoutConfig(zk, input_topic, "",
				input_topic);
		kafkaConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
		// kafkaConfig.forceFromStart = Boolean.valueOf("True");
		kafkaConfig.startOffsetTime = -1;

		builder.setSpout("kafka-spout", new KafkaSpout(kafkaConfig),
				config.getInt("spout.kafka.parallelism.hint")).setNumTasks(
				config.getInt("spout.kafka.num.tasks"));*/


		// Testing Spout
		
		  GenericInternalTestSpout testSpout = new GenericInternalTestSpout()
		  .withFilename("SampleInput/BroExampleOutput").withRepeating(false);
		  
		  builder.setSpout("EnrichmentSpout", testSpout,
		  config.getInt("spout.test.parallelism.hint")).setNumTasks(
		  config.getInt("spout.test.num.tasks"));
		 

		// ------------ParserBolt configuration

		AbstractParserBolt parser_bolt = new TelemetryParserBolt()
				.withMessageParser(new BasicBroParser()).withOutputFieldName(
						topology_name).withMetricConfig(config);
						

		builder.setBolt("ParserBolt", parser_bolt,
				config.getInt("bolt.parser.parallelism.hint"))
				.shuffleGrouping("EnrichmentSpout")
				.setNumTasks(config.getInt("bolt.parser.num.tasks"));
		
		// ------------Geo Enrichment Bolt Configuration

	/*	List<String> geo_keys = new ArrayList<String>();
		geo_keys.add(config.getString("bolt.enrichment.geo.source_ip"));
		geo_keys.add(config.getString("bolt.enrichment.geo.resp_ip"));

		GeoMysqlAdapter geo_adapter = new GeoMysqlAdapter(
				config.getString("bolt.enrichment.geo.adapter.ip"),
				config.getInt("bolt.enrichment.geo.adapter.port"),
				config.getString("bolt.enrichment.geo.adapter.username"),
				config.getString("bolt.enrichment.geo.adapter.password"),
				config.getString("bolt.enrichment.geo.adapter.table"));

		GenericEnrichmentBolt geo_enrichment = new GenericEnrichmentBolt()
				.withEnrichmentTag(
						config.getString("bolt.enrichment.geo.enrichment_tag"))
				.withOutputFieldName(topology_name)
				.withAdapter(geo_adapter)
				.withMaxTimeRetain(
						config.getInt("bolt.enrichment.geo.MAX_TIME_RETAIN"))
				.withMaxCacheSize(
						config.getInt("bolt.enrichment.geo.MAX_CACHE_SIZE"))
				.withKeys(geo_keys).withMetricConfiguration(config);

		builder.setBolt("GeoEnrichBolt", geo_enrichment,
				config.getInt("bolt.enrichment.geo.parallelism.hint"))
				.shuffleGrouping("ParserBolt")
				.setNumTasks(config.getInt("bolt.enrichment.geo.num.tasks"));*/
		
		// ------------Hosts Enrichment Bolt Configuration
		
	/*	Configuration hosts = new PropertiesConfiguration("TopologyConfigs/known_hosts/known_hosts.conf");
		
		Iterator<String> keys = hosts.getKeys();
		Map<String, JSONObject> known_hosts = new HashMap<String, JSONObject>();
		JSONParser parser = new JSONParser();
		 
		    while(keys.hasNext())
		    {
		    	String key = keys.next().trim();
		    	JSONArray value = (JSONArray) parser.parse(hosts.getProperty(key).toString());
		    	known_hosts.put(key, (JSONObject) value.get(0));
		    }
		
		HostAdapter host_adapter = new HostAdapter(known_hosts);
		
		GenericEnrichmentBolt host_enrichment = new GenericEnrichmentBolt().withEnrichmentTag(config.getString("bolt.enrichment.host.enrichment_tag")).
				withAdapter(host_adapter).withMaxTimeRetain(
						config.getInt("bolt.enrichment.host.MAX_TIME_RETAIN"))
				.withMaxCacheSize(
						config.getInt("bolt.enrichment.host.MAX_CACHE_SIZE")).withOutputFieldName(topology_name)
						.withKeys(geo_keys).withMetricConfiguration(config);

		builder.setBolt("HostEnrichBolt", host_enrichment,
				config.getInt("bolt.enrichment.host.parallelism.hint"))
				.shuffleGrouping("GeoEnrichBolt")
				.setNumTasks(config.getInt("bolt.enrichment.host.num.tasks"));*/
		
		// -------------Alerts Bolt
		
/*		Configuration alert_rules = new PropertiesConfiguration("TopologyConfigs/alerts/alerts.conf");
		
		Iterator<String> rules_itr = hosts.getKeys();
		Map<String, JSONObject> rules = new HashMap<String, JSONObject>();
		JSONParser pr = new JSONParser();
		 
		    while(rules_itr.hasNext())
		    {
		    	String key = rules_itr.next().trim();
		    	JSONArray value = (JSONArray) pr.parse(hosts.getProperty(key).toString());
		    	rules.put(key, (JSONObject) value.get(0));
		    }
		*/
		    
		// ------------Whois Enrichment Bolt Configuration

		List<String> whois_keys = new ArrayList<String>();
		String[] keys_from_settings = config.getString("bolt.enrichment.whois.source").split(",");
		
		for(String key : keys_from_settings)
			whois_keys.add(key);

		EnrichmentAdapter whois_adapter = new WhoisHBaseAdapter(
				config.getString("bolt.enrichment.whois.hbase.table.name"),
				config.getString("kafka.zk.list"),
				config.getString("kafka.zk.port"));

		GenericEnrichmentBolt whois_enrichment = new GenericEnrichmentBolt()
				.withEnrichmentTag(
						config.getString("bolt.enrichment.whois.enrichment_tag"))
				.withOutputFieldName(topology_name)
				.withAdapter(whois_adapter)
				.withMaxTimeRetain(
						config.getInt("bolt.enrichment.whois.MAX_TIME_RETAIN"))
				.withMaxCacheSize(
						config.getInt("bolt.enrichment.whois.MAX_CACHE_SIZE")).withKeys(whois_keys)
						.withMetricConfiguration(config);

		builder.setBolt("WhoisEnrichBolt", whois_enrichment,
				config.getInt("bolt.enrichment.whois.parallelism.hint"))
				.shuffleGrouping("ParserBolt")
				.setNumTasks(config.getInt("bolt.enrichment.whois.num.tasks"));
		

		// ------------CIF bolt configuration

		/*
		 * Map<String, Pattern> cif_patterns = new HashMap<String, Pattern>();
		 * cif_patterns.put("source_ip", Pattern.compile(config
		 * .getString("bolt.enrichment.cif.source_ip")));
		 * cif_patterns.put("resp_ip", Pattern.compile(config
		 * .getString("bolt.enrichment.cif.resp_ip"))); cif_patterns.put("host",
		 * Pattern.compile(config.getString("bolt.enrichment.cif.host")));
		 * cif_patterns.put("email",
		 * Pattern.compile(config.getString("bolt.enrichment.cif.email")));
		 */

		// Add all CIF json keys that need are used for CIF enhancement.

		List<String> cif_keys = new ArrayList<String>();

		cif_keys.add(config.getString("bolt.enrichment.cif.source_ip"));
		cif_keys.add(config.getString("bolt.enrichment.cif.resp_ip"));
		cif_keys.add(config.getString("bolt.enrichment.cif.host"));
		cif_keys.add(config.getString("bolt.enrichment.cif.email"));

		GenericEnrichmentBolt cif_enrichment = new GenericEnrichmentBolt()
		.withEnrichmentTag(
				config.getString("bolt.enrichment.cif.enrichment_tag"))
				.withAdapter(
						new CIFHbaseAdapter(config.getString("kafka.zk.list"),
								config.getString("kafka.zk.port"),config.getString("bolt.enrichment.cif.tablename")))
				.withOutputFieldName(topology_name)
				.withEnrichmentTag("CIF_Enrichment")
				.withKeys(cif_keys)
				.withMaxTimeRetain(
						config.getInt("bolt.enrichment.cif.MAX_TIME_RETAIN"))
				.withMaxCacheSize(
						config.getInt("bolt.enrichment.cif.MAX_CACHE_SIZE"))
						.withMetricConfiguration(config);

		builder.setBolt("CIFEnrichmentBolt", cif_enrichment,
				config.getInt("bolt.enrichment.cif.parallelism.hint"))
				.shuffleGrouping("WhoisEnrichBolt")
				.setNumTasks(config.getInt("bolt.enrichment.cif.num.tasks"));

		// ------------Kafka Bolt Configuration

		Map<String, String> kafka_broker_properties = new HashMap<String, String>();
		kafka_broker_properties.put("zk.connect", config.getString("kafka.zk"));
		kafka_broker_properties.put("metadata.broker.list",
				config.getString("kafka.br"));
		
		kafka_broker_properties.put("serializer.class",
				"com.opensoc.json.serialization.JSONKafkaSerializer");
		
		String output_topic = config.getString("bolt.kafka.topic");

		conf.put("kafka.broker.properties", kafka_broker_properties);
		conf.put("topic", output_topic);

		builder.setBolt("KafkaBolt", new KafkaBolt<String, String>(),
				config.getInt("bolt.kafka.parallelism.hint"))
				.shuffleGrouping("CIFEnrichmentBolt")
				.setNumTasks(config.getInt("bolt.kafka.num.tasks"));

		// ------------Indexing BOLT configuration

		TelemetryIndexingBolt indexing_bolt = new TelemetryIndexingBolt()
				.withIndexIP(config.getString("bolt.indexing.indexIP"))
				.withIndexPort(config.getInt("bolt.indexing.port"))
				.withClusterName(config.getString("bolt.indexing.clustername"))
				.withIndexName(config.getString("bolt.indexing.indexname"))
				.withDocumentName(
						config.getString("bolt.indexing.documentname"))
				.withBulk(config.getInt("bolt.indexing.bulk"))
				.withOutputFieldName(topology_name)
				.withIndexAdapter(new ESBaseBulkAdapter())
				.withMetricConfiguration(config)
				;

		builder.setBolt("IndexingBolt", indexing_bolt,
				config.getInt("bolt.indexing.parallelism.hint"))
				.shuffleGrouping("CIFEnrichmentBolt")
				.setNumTasks(config.getInt("bolt.indexing.num.tasks"));


		// * ------------HDFS BOLT configuration

	/*	FileNameFormat fileNameFormat = new DefaultFileNameFormat()
				.withPath("/" + topology_name + "/");
		RecordFormat format = new DelimitedRecordFormat()
				.withFieldDelimiter("|");

		SyncPolicy syncPolicy = new CountSyncPolicy(5);
		FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(config.getFloat("bolt.hdfs.size.rotation.policy"),
				Units.KB);

		HdfsBolt hdfsBolt = new HdfsBolt().withFsUrl(config.getString("bolt.hdfs.fs.url"))
				.withFileNameFormat(fileNameFormat).withRecordFormat(format)
				.withRotationPolicy(rotationPolicy).withSyncPolicy(syncPolicy);

		builder.setBolt("HDFSBolt", hdfsBolt, config.getInt("bolt.hdfs.parallelism.hint"))
				.shuffleGrouping("CIFEnrichmentBolt").setNumTasks(config.getInt("bolt.hdfs.num.tasks"));*/
		
		
		// * ------------HDFS BOLT For Enriched Data configuration

			/*	FileNameFormat fileNameFormat_enriched = new DefaultFileNameFormat()
						.withPath("/" + topology_name + "_enriched/");
				RecordFormat format_enriched = new DelimitedRecordFormat()
						.withFieldDelimiter("|");

				SyncPolicy syncPolicy_enriched = new CountSyncPolicy(5);
				FileRotationPolicy rotationPolicy_enriched = new FileSizeRotationPolicy(config.getFloat("bolt.hdfs.size.rotation.policy"),
						Units.KB);

				HdfsBolt hdfsBolt_enriched = new HdfsBolt().withFsUrl(config.getString("bolt.hdfs.fs.url"))
						.withFileNameFormat(fileNameFormat_enriched).withRecordFormat(format_enriched)
						.withRotationPolicy(rotationPolicy_enriched).withSyncPolicy(syncPolicy_enriched);

				builder.setBolt("HDFSBolt_enriched", hdfsBolt_enriched, config.getInt("bolt.hdfs.parallelism.hint"))
						.shuffleGrouping("CIFEnrichmentBolt").setNumTasks(config.getInt("bolt.hdfs.num.tasks"));*/


		if (config.getBoolean("local.mode")) {
			conf.setNumWorkers(config.getInt("num.workers"));
			conf.setMaxTaskParallelism(1);
			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology(topology_name, conf,
					builder.createTopology());
		} else {

			conf.setNumWorkers(config.getInt("num.workers"));
			StormSubmitter.submitTopology(topology_name, conf,
					builder.createTopology());
		}
	}
}
