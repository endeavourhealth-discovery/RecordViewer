DROP SCHEMA hl7v2_inbound;

CREATE SCHEMA hl7v2_inbound;

USE hl7v2_inbound;

CREATE TABLE `imperial` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `date_received` datetime DEFAULT NULL,
  `message_wrapper` text,
  `hl7_message` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
