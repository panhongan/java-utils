package com.github.panhongan.utils.kafka.export;

import java.util.ArrayList;
import java.util.List;

import com.github.panhongan.utils.conf.Config;
import com.github.panhongan.utils.control.Lifecycleable;
import com.github.panhongan.utils.kafka.AbstractKafkaMessageProcessor;
import com.github.panhongan.utils.kafka.HighLevelConsumerGroup;
import com.github.panhongan.utils.kafka.MessageLocalWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
			String[] arr = config.getString("kafka.topic.partition").split(":");
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
