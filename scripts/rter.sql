-- MySQL dump 10.13  Distrib 5.5.33, for Linux (x86_64)
--
-- Host: localhost    Database: rter
-- ------------------------------------------------------
-- Server version	5.5.33

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
-- Table structure for table `Geolocations`
--

DROP TABLE IF EXISTS `Geolocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Geolocations` (
  `ItemID` int(11) NOT NULL,
  `Lat` decimal(9,6) DEFAULT NULL,
  `Lng` decimal(9,6) DEFAULT NULL,
  `Heading` decimal(9,6) DEFAULT NULL,
  `Radius` decimal(12,6) DEFAULT NULL,
  `Timestamp` datetime NOT NULL DEFAULT '0001-01-01 00:00:00',
  KEY `FK_Geolocations_Items` (`ItemID`),
  CONSTRAINT `FK_Geolocations_Items` FOREIGN KEY (`ItemID`) REFERENCES `Items` (`ID`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ItemComments`
--

DROP TABLE IF EXISTS `ItemComments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ItemComments` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `ItemID` int(11) NOT NULL,
  `Author` varchar(64) NOT NULL,
  `Body` text NOT NULL,
  `UpdateTime` datetime NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `ItemID` (`ItemID`),
  KEY `Author` (`Author`),
  CONSTRAINT `itemcomments_ibfk_1` FOREIGN KEY (`ItemID`) REFERENCES `Items` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `itemcomments_ibfk_2` FOREIGN KEY (`Author`) REFERENCES `Users` (`Username`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Items`
--

DROP TABLE IF EXISTS `Items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Items` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `Type` varchar(64) NOT NULL,
  `Author` varchar(64) NOT NULL,
  `ThumbnailURI` varchar(2048) NOT NULL DEFAULT '',
  `ContentURI` varchar(2048) NOT NULL DEFAULT '',
  `UploadURI` varchar(2048) NOT NULL DEFAULT '',
  `ContentToken` varchar(2048) NOT NULL DEFAULT '',
  `HasHeading` tinyint(1) NOT NULL DEFAULT '0',
  `Heading` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `HasGeo` tinyint(1) NOT NULL DEFAULT '0',
  `Lat` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `Lng` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `Radius` decimal(12,6) NOT NULL DEFAULT '0.000000',
  `Live` tinyint(1) NOT NULL DEFAULT '0',
  `StartTime` datetime NOT NULL DEFAULT '0001-01-01 00:00:00',
  `StopTime` datetime NOT NULL DEFAULT '0001-01-01 00:00:00',
  PRIMARY KEY (`ID`),
  KEY `Author` (`Author`),
  CONSTRAINT `items_ibfk_1` FOREIGN KEY (`Author`) REFERENCES `Users` (`Username`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=395 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Roles`
--

DROP TABLE IF EXISTS `Roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Roles` (
  `Title` varchar(64) NOT NULL,
  `Permissions` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`Title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TermRankings`
--

DROP TABLE IF EXISTS `TermRankings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TermRankings` (
  `Term` varchar(64) NOT NULL,
  `Ranking` text NOT NULL,
  `UpdateTime` datetime NOT NULL,
  PRIMARY KEY (`Term`),
  CONSTRAINT `termrankings_ibfk_1` FOREIGN KEY (`Term`) REFERENCES `Terms` (`Term`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TermRelationships`
--

DROP TABLE IF EXISTS `TermRelationships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TermRelationships` (
  `Term` varchar(64) NOT NULL,
  `ItemID` int(11) NOT NULL,
  PRIMARY KEY (`Term`,`ItemID`),
  KEY `ItemID` (`ItemID`),
  CONSTRAINT `termrelationships_ibfk_1` FOREIGN KEY (`Term`) REFERENCES `Terms` (`Term`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `termrelationships_ibfk_2` FOREIGN KEY (`ItemID`) REFERENCES `Items` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Terms`
--

DROP TABLE IF EXISTS `Terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Terms` (
  `Term` varchar(64) NOT NULL,
  `Automated` tinyint(1) NOT NULL DEFAULT '0',
  `Author` varchar(64) NOT NULL,
  `UpdateTime` datetime NOT NULL,
  PRIMARY KEY (`Term`),
  KEY `Author` (`Author`),
  CONSTRAINT `terms_ibfk_1` FOREIGN KEY (`Author`) REFERENCES `Users` (`Username`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UserDirections`
--

DROP TABLE IF EXISTS `UserDirections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `UserDirections` (
  `Username` varchar(64) NOT NULL,
  `LockUsername` varchar(64) NOT NULL DEFAULT '',
  `Command` varchar(64) NOT NULL DEFAULT 'none',
  `Heading` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `Lat` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `Lng` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `UpdateTime` datetime NOT NULL,
  PRIMARY KEY (`Username`),
  CONSTRAINT `userdirections_ibfk_1` FOREIGN KEY (`Username`) REFERENCES `Users` (`Username`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Users`
--

DROP TABLE IF EXISTS `Users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Users` (
  `Username` varchar(64) NOT NULL,
  `Password` char(128) NOT NULL,
  `Salt` char(128) NOT NULL,
  `Role` varchar(64) NOT NULL DEFAULT 'public',
  `TrustLevel` int(11) NOT NULL DEFAULT '0',
  `CreateTime` datetime NOT NULL DEFAULT '0001-01-01 00:00:00',
  `Heading` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `Lat` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `Lng` decimal(9,6) NOT NULL DEFAULT '0.000000',
  `UpdateTime` datetime NOT NULL DEFAULT '0001-01-01 00:00:00',
  `Status` varchar(64) NOT NULL,
  `StatusTime` datetime NOT NULL DEFAULT '0001-01-01 00:00:00',
  PRIMARY KEY (`Username`),
  UNIQUE KEY `Username` (`Username`),
  KEY `Role` (`Role`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`Role`) REFERENCES `Roles` (`Title`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Users_Bkp`
--

DROP TABLE IF EXISTS `Users_Bkp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Users_Bkp` (
  `Username` varchar(64) NOT NULL,
  `Password` char(128) NOT NULL,
  `Salt` char(128) NOT NULL,
  `Role` varchar(64) NOT NULL DEFAULT 'public',
  `TrustLevel` int(11) NOT NULL DEFAULT '0',
  `CreateTime` datetime NOT NULL,
  PRIMARY KEY (`Username`),
  UNIQUE KEY `Username` (`Username`),
  KEY `Role` (`Role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-01-22  1:12:57
