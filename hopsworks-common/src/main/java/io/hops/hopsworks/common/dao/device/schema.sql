
CREATE TABLE `project_devices`(
	`project_id` INT(11) NOT NULL,
	`device_uuid` VARCHAR(36) NOT NULL,
	`password` VARCHAR(64) NOT NULL,
	`alias` VARCHAR(80) NOT NULL,
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`state` TINYINT(1) NOT NULL DEFAULT '0',
	`last_logged_in` TIMESTAMP NULL DEFAULT NULL,
	PRIMARY KEY (`project_id`, `device_uuid`),
	FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1;

CREATE TABLE `project_devices_settings` (
	`project_id` INT(11) NOT NULL,
	`jwt_secret` VARCHAR(128) NOT NULL,
	`jwt_token_duration` INT(11) NOT NULL,
	PRIMARY KEY (`project_id`),
	FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1;
