package com.github.lalalu.utils.kafka.export;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lalalu.utils.conf.Config;
import com.github.lalalu.utils.control.Lifecycleable;
import com.github.lalalu.utils.kafka.AbstractKafkaMessageProcessor;
import com.github.lalalu.utils.kafka.HighLevelConsumerGroup;
import com.github.lalalu.utils.kafka.MessageLocalWriter;

/**
 * lalalu plus
 */
public class KafkaExporter implements Lifecycleable {
	
	private static final Logger logger = LoggerFactory.getLogger(KafkaExporter.class);
	
	private static KafkaExporterConfig kafka_exporter_config = KafkaExporterConfig.getInstance();
	
	private List<AbstractKafkaMessageProcessor> processors = new ArrayList<>();
	
	private HighLevelConsumerGroup group = null;
	
	@Override
	public boolean init() {
		boolean is_ok = false;
		
		Config config = kafka_exporter_config.getConfig();
		
		try {
			String [] arr = config.getString("kafka.topic.partition").split(":");
			String topic = arr[0];
			int partitions = Integer.valueOf(arr[1]).intValue();
			int minutes_window = config.getInt("local.data.minutes.window", 10);
			
			for (int i = 0; i < partitions; ++i) {
				MessageLocalWriter local_writer = new MessageLocalWriter(config.getString("local.data.dir"), minutes_window);
				if (local_writer.init()) {
					processors.add(local_writer);
				} else {
					logger.warn("MessageLocalWriter init failed");
				}
			}
			
			is_ok = (processors.size() == partitions);
			if (is_ok) {
				group = new HighLevelConsumerGroup(config.getString("kafka.zk.list"), 
						config.getString("kafka.consumer.group"),
						topic, partitions, 
						Boolean.valueOf(config.getString("kafka.consumer.group.restart.offset.largest")), 
						processors);
				if (group.init()) {
					logger.info("HighLevelConsumerGroup init ok");
					is_ok = true;
				} else {
					logger.warn("HighLevelConsumerGroup init failed");
				}
			}
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}
		
		return is_ok;
	}
	
	@Override
	public void uninit() {
		if (group != null) {
			group.uninit();
			logger.info("HighLevelConsumerGroup uninit");
		}
		
		for (AbstractKafkaMessageProcessor processor : processors) {
			processor.uninit();
			logger.info("MessageProcessor uninit : {}", processor.getName());
		}
	}
	
}
