-- MySQL dump 10.13  Distrib 5.6.22, for osx10.8 (x86_64)
--
-- Host: localhost    Database: kthfs
-- ------------------------------------------------------
-- Server version	5.6.23

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `activity`
--

DROP TABLE IF EXISTS `activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `activity` varchar(128) DEFAULT NULL,
  `performed_by` varchar(255) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `flag` enum('data','study','team','users') DEFAULT NULL,
  `activity_on` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity`
--

LOCK TABLES `activity` WRITE;
/*!40000 ALTER TABLE `activity` DISABLE KEYS */;
/*!40000 ALTER TABLE `activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `address`
--

DROP TABLE IF EXISTS `address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `address` (
  `address1` varchar(120) DEFAULT NULL,
  `address2` varchar(120) DEFAULT NULL,
  `address3` varchar(120) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `state` varchar(100) DEFAULT NULL,
  `country` varchar(100) DEFAULT NULL,
  `postalcode` varchar(16) DEFAULT NULL,
  `address_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `uid` int(11) NOT NULL,
  PRIMARY KEY (`address_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `address`
--

LOCK TABLES `address` WRITE;
/*!40000 ALTER TABLE `address` DISABLE KEYS */;
/*!40000 ALTER TABLE `address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `groups`
--

DROP TABLE IF EXISTS `groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `groups` (
  `gid` int(10) NOT NULL,
  `group_name` varchar(20) NOT NULL,
  `group_desc` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`gid`),
  UNIQUE KEY `group_name_UNIQUE` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `groups`
--

LOCK TABLES `groups` WRITE;
/*!40000 ALTER TABLE `groups` DISABLE KEYS */;
INSERT INTO `groups` VALUES (0,'ADMIN','Administrators'),(1,'USER','Users');
/*!40000 ALTER TABLE `groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inodes`
--

DROP TABLE IF EXISTS `inodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `inodes` (
  `id` mediumint(9) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `pid` mediumint(9) DEFAULT NULL,
  `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `isdir` tinyint(1) NOT NULL,
  `size` int(11) DEFAULT NULL,
  `status` enum('uploading','copying_to_hdfs','available') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pid_3` (`pid`,`name`),
  KEY `pid` (`pid`),
  KEY `pid_2` (`pid`,`isdir`),
  KEY `name` (`name`),
  CONSTRAINT `inodes_ibfk_1` FOREIGN KEY (`pid`) REFERENCES `inodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inodes`
--

LOCK TABLES `inodes` WRITE;
/*!40000 ALTER TABLE `inodes` DISABLE KEYS */;
/*!40000 ALTER TABLE `inodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_execution_files`
--

DROP TABLE IF EXISTS `job_execution_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_execution_files` (
  `job_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_execution_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_execution_files`
--

LOCK TABLES `job_execution_files` WRITE;
/*!40000 ALTER TABLE `job_execution_files` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_execution_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_input_files`
--

DROP TABLE IF EXISTS `job_input_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_input_files` (
  `job_id` bigint(20) unsigned NOT NULL,
  `path` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_input_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_input_files`
--

LOCK TABLES `job_input_files` WRITE;
/*!40000 ALTER TABLE `job_input_files` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_input_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_output_files`
--

DROP TABLE IF EXISTS `job_output_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_output_files` (
  `job_id` bigint(20) unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `path` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`name`),
  CONSTRAINT `job_output_files_ibfk_1` FOREIGN KEY (`job_id`) REFERENCES `jobhistory` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_output_files`
--

LOCK TABLES `job_output_files` WRITE;
/*!40000 ALTER TABLE `job_output_files` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_output_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jobhistory`
--

DROP TABLE IF EXISTS `jobhistory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jobhistory` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(128) DEFAULT NULL,
  `submission_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `study` varchar(128) NOT NULL,
  `user` varchar(255) NOT NULL,
  `state` varchar(128) NOT NULL,
  `execution_duration` bigint(20) DEFAULT NULL,
  `args` varchar(255) DEFAULT NULL,
  `stdout_path` varchar(255) DEFAULT NULL,
  `stderr_path` varchar(255) DEFAULT NULL,
  `type` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `fk_jobhistory_user` (`user`),
  KEY `fk_jobhistory_study` (`study`),
  CONSTRAINT `fk_jobhistory_user` FOREIGN KEY (`user`) REFERENCES `users` (`email`),
  CONSTRAINT `jobhistory_ibfk_3` FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jobhistory`
--

LOCK TABLES `jobhistory` WRITE;
/*!40000 ALTER TABLE `jobhistory` DISABLE KEYS */;
/*!40000 ALTER TABLE `jobhistory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login`
--

DROP TABLE IF EXISTS `login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `login` (
  `loginid` bigint(20) NOT NULL AUTO_INCREMENT,
  `people_uid` int(10) DEFAULT NULL,
  `last_login` timestamp NULL DEFAULT NULL,
  `last_ip` varchar(45) DEFAULT NULL,
  `os_platform` int(11) DEFAULT NULL,
  `logout` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`loginid`),
  KEY `fk_login_people` (`people_uid`),
  CONSTRAINT `fk_login_people` FOREIGN KEY (`people_uid`) REFERENCES `people` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_login_people_uid` FOREIGN KEY (`people_uid`) REFERENCES `people` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login`
--

LOCK TABLES `login` WRITE;
/*!40000 ALTER TABLE `login` DISABLE KEYS */;
/*!40000 ALTER TABLE `login` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `people`
--

DROP TABLE IF EXISTS `people`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `people` (
  `uid` int(10) NOT NULL DEFAULT '1000',
  `username` varchar(10) NOT NULL,
  `password` varchar(128) NOT NULL,
  `email` varchar(45) DEFAULT NULL,
  `fname` varchar(30) DEFAULT NULL,
  `lname` varchar(30) DEFAULT NULL,
  `activated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `home_org` varchar(100) DEFAULT NULL,
  `title` varchar(10) DEFAULT NULL,
  `mobile` varchar(20) DEFAULT NULL,
  `orcid` varchar(20) DEFAULT NULL,
  `false_login` int(11) NOT NULL DEFAULT '0',
  `active` int(11) NOT NULL DEFAULT '-1',
  `isonline` int(11) NOT NULL DEFAULT '-1',
  `secret` varchar(20) DEFAULT NULL,
  `security_question` varchar(20) DEFAULT NULL,
  `security_answer` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `people`
--

LOCK TABLES `people` WRITE;
/*!40000 ALTER TABLE `people` DISABLE KEYS */;
/*!40000 ALTER TABLE `people` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `people_group`
--

DROP TABLE IF EXISTS `people_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `people_group` (
  `uid` int(10) NOT NULL,
  `gid` int(10) NOT NULL,
  PRIMARY KEY (`uid`,`gid`),
  KEY `fk_people_has_group_group1` (`gid`),
  KEY `fk_people_has_groups_people` (`uid`),
  CONSTRAINT `fk_group` FOREIGN KEY (`gid`) REFERENCES `groups` (`gid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_people` FOREIGN KEY (`uid`) REFERENCES `people` (`uid`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_people_group_uid` FOREIGN KEY (`uid`) REFERENCES `people` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `people_group`
--

LOCK TABLES `people_group` WRITE;
/*!40000 ALTER TABLE `people_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `people_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(45) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `dateCreated` datetime DEFAULT NULL,
  `status` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (1,'HumanGenome','TESTING TO CHANGE VALUE',NULL,1),(2,'FinanceCalc','Finance description',NULL,0),(26,'asdasd','asdasda',NULL,0),(31,'SpecialProject','SuperDuper project','2015-04-14 18:22:41',0),(32,'Andre Project','This is the shit...','2015-04-14 18:23:24',0),(33,'ErmiasTesting','This is some shorthand description...','2015-04-14 18:24:26',0);
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_history`
--

DROP TABLE IF EXISTS `project_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `projectID` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `operation` enum('ACCESS','UPDATE','CREATE') NOT NULL,
  `date` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_history_fk_idx` (`projectID`),
  KEY `project_history_email_fk_idx` (`email`),
  CONSTRAINT `project_history_email_fk` FOREIGN KEY (`email`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `project_history_project_fk` FOREIGN KEY (`projectID`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_history`
--

LOCK TABLES `project_history` WRITE;
/*!40000 ALTER TABLE `project_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_role`
--

DROP TABLE IF EXISTS `project_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_role` (
  `name` varchar(45) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_role`
--

LOCK TABLES `project_role` WRITE;
/*!40000 ALTER TABLE `project_role` DISABLE KEYS */;
INSERT INTO `project_role` VALUES ('CONTRIBUTOR','All active participants who has READ/WRITE access'),('GUEST','All folks that has READ access'),('OWNER','The one that creates the project');
/*!40000 ALTER TABLE `project_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_type`
--

DROP TABLE IF EXISTS `project_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `projectID` int(11) NOT NULL,
  `type` enum('CUNEIFORM','SAMPLES','PROJECT_INFO','SPARK','ADAM','MAPREDUCE','YARN','ZEPPELIN') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `project_type_fk_idx` (`projectID`),
  CONSTRAINT `project_type_fk` FOREIGN KEY (`projectID`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_type`
--

LOCK TABLES `project_type` WRITE;
/*!40000 ALTER TABLE `project_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_user`
--

DROP TABLE IF EXISTS `project_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `projectId` int(11) NOT NULL,
  `role` varchar(45) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_project` (`email`,`projectId`),
  KEY `role_fk_idx` (`role`),
  KEY `project_fk_idx` (`projectId`),
  KEY `user_fk_idx` (`email`),
  CONSTRAINT `project_fk` FOREIGN KEY (`projectId`) REFERENCES `project` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `role_fk` FOREIGN KEY (`role`) REFERENCES `project_role` (`name`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `user_email_fk` FOREIGN KEY (`email`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_user`
--

LOCK TABLES `project_user` WRITE;
/*!40000 ALTER TABLE `project_user` DISABLE KEYS */;
INSERT INTO `project_user` VALUES (1,'amore@kth.se',1,'OWNER'),(2,'amore@kth.se',2,'GUEST'),(33,'ermiasg@kth.se',26,'GUEST'),(41,'ermiasg@kth.se',31,'GUEST'),(42,'amore@kth.se',31,'OWNER'),(43,'amore@kth.se',32,'OWNER'),(44,'ermiasg@kth.se',33,'GUEST'),(45,'amore@kth.se',33,'OWNER');
/*!40000 ALTER TABLE `project_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `study`
--

DROP TABLE IF EXISTS `study`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `study` (
  `name` varchar(128) NOT NULL,
  `username` varchar(255) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`),
  UNIQUE KEY `name` (`name`),
  KEY `fk_study_users` (`username`),
  CONSTRAINT `fk_study_users` FOREIGN KEY (`username`) REFERENCES `users` (`email`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study`
--

LOCK TABLES `study` WRITE;
/*!40000 ALTER TABLE `study` DISABLE KEYS */;
/*!40000 ALTER TABLE `study` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `study_services`
--

DROP TABLE IF EXISTS `study_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `study_services` (
  `study` varchar(128) NOT NULL,
  `service` varchar(32) NOT NULL,
  PRIMARY KEY (`study`,`service`),
  CONSTRAINT `study_services_ibfk_1` FOREIGN KEY (`study`) REFERENCES `study` (`name`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `study_services`
--

LOCK TABLES `study_services` WRITE;
/*!40000 ALTER TABLE `study_services` DISABLE KEYS */;
/*!40000 ALTER TABLE `study_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `email` varchar(255) NOT NULL,
  `telephoneNum` varchar(255) DEFAULT NULL,
  `firstName` varchar(255) DEFAULT NULL,
  `password` varchar(128) NOT NULL,
  `registeredon` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `salt` longblob,
  `status` int(11) NOT NULL DEFAULT '0',
  `lastName` varchar(255) DEFAULT NULL,
  `security_question` varchar(45) DEFAULT NULL,
  `security_answer` varchar(128) DEFAULT NULL,
  `secret` varchar(45) DEFAULT NULL,
  `isonline` int(11) NOT NULL DEFAULT '0',
  `false_login` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES ('amore@kth.se','0737291225','Andre','6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b','2015-04-07 15:40:00',NULL,1,'More','How are you','Good',NULL,0,0),('ermiasg@kth.se','0700000000','Ermias','6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b','2015-04-07 15:40:48',NULL,1,'Gebremeskel','Who','This',NULL,0,0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users_groups`
--

DROP TABLE IF EXISTS `users_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users_groups` (
  `email` varchar(255) NOT NULL,
  `groupname` varchar(64) NOT NULL,
  UNIQUE KEY `unq_users_groups_0` (`email`,`groupname`),
  KEY `group_idx` (`groupname`),
  CONSTRAINT `group_fk` FOREIGN KEY (`groupname`) REFERENCES `groups` (`group_name`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `user_fk` FOREIGN KEY (`email`) REFERENCES `users` (`email`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users_groups`
--

LOCK TABLES `users_groups` WRITE;
/*!40000 ALTER TABLE `users_groups` DISABLE KEYS */;
INSERT INTO `users_groups` VALUES ('amore@kth.se','USER'),('ermiasg@kth.se','USER');
/*!40000 ALTER TABLE `users_groups` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-04-15 12:19:32
