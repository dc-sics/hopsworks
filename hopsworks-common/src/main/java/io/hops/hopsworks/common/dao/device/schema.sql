
CREATE TABLE `project_devices`(
	`project_id` INT(11) NOT NULL,
	`device_uuid` VARCHAR(36) NOT NULL,
	`pass_uuid` VARCHAR(36) NOT NULL,
	`user_id` INT(11) NOT NULL,
	`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`enabled` TINYINT(1) NOT NULL DEFAULT '1',
	`last_produced` TIMESTAMP DEFAULT NULL,
	`produce_counter` INT(11) DEFAULT '0',
	PRIMARY KEY (`project_id`, `device_uuid`),
	FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
	FOREIGN KEY (`user_id`) REFERENCES `users` (`uid`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1;

CREATE TABLE `project_secrets` (
	`project_id` INT(11) NOT NULL,
	`jwt_secret` VARCHAR(128) NOT NULL,
	`jwt_token_duration` INT(11) NOT NULL,
	PRIMARY KEY (`project_id`),
	FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=ndbcluster DEFAULT CHARSET=latin1;
