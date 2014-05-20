-- --------------------------------------------------------
-- Host:                         rter.zapto.org
-- Server version:               5.5.33 - MySQL Community Server (GPL)
-- Server OS:                    Linux
-- HeidiSQL Version:             8.1.0.4545
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


-- Dumping structure for table rter.Geolocations
DROP TABLE IF EXISTS `Geolocations`;
CREATE TABLE IF NOT EXISTS `Geolocations` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `ItemID` int(11) NOT NULL,
  `Lat` decimal(9,6) DEFAULT NULL,
  `Lng` decimal(9,6) DEFAULT NULL,
  `Heading` decimal(9,6) DEFAULT NULL,
  `Radius` decimal(12,6) DEFAULT NULL,
  `Timestamp` datetime NOT NULL DEFAULT '0001-01-01 00:00:00',
  PRIMARY KEY (`ID`),
  KEY `FK_Geolocations_Items` (`ItemID`),
  CONSTRAINT `FK_Geolocations_Items` FOREIGN KEY (`ItemID`) REFERENCES `Items` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.


-- Dumping structure for table rter.ItemComments
DROP TABLE IF EXISTS `ItemComments`;
CREATE TABLE IF NOT EXISTS `ItemComments` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table rter.Items
DROP TABLE IF EXISTS `Items`;
CREATE TABLE IF NOT EXISTS `Items` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table rter.Roles
DROP TABLE IF EXISTS `Roles`;
CREATE TABLE IF NOT EXISTS `Roles` (
  `Title` varchar(64) NOT NULL,
  `Permissions` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`Title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
INSERT INTO Roles (Title, Permissions) VALUES ("public", 1), ("observer", 1), ("responder", 3), ("editor", 7), ("admin", 15);

-- Data exporting was unselected.


-- Dumping structure for table rter.TermRankings
DROP TABLE IF EXISTS `TermRankings`;
CREATE TABLE IF NOT EXISTS `TermRankings` (
  `Term` varchar(64) NOT NULL,
  `Ranking` text NOT NULL,
  `UpdateTime` datetime NOT NULL,
  PRIMARY KEY (`Term`),
  CONSTRAINT `termrankings_ibfk_1` FOREIGN KEY (`Term`) REFERENCES `Terms` (`Term`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table rter.TermRelationships
DROP TABLE IF EXISTS `TermRelationships`;
CREATE TABLE IF NOT EXISTS `TermRelationships` (
  `Term` varchar(64) NOT NULL,
  `ItemID` int(11) NOT NULL,
  PRIMARY KEY (`Term`,`ItemID`),
  KEY `ItemID` (`ItemID`),
  CONSTRAINT `termrelationships_ibfk_1` FOREIGN KEY (`Term`) REFERENCES `Terms` (`Term`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `termrelationships_ibfk_2` FOREIGN KEY (`ItemID`) REFERENCES `Items` (`ID`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table rter.Terms
DROP TABLE IF EXISTS `Terms`;
CREATE TABLE IF NOT EXISTS `Terms` (
  `Term` varchar(64) NOT NULL,
  `Automated` tinyint(1) NOT NULL DEFAULT '0',
  `Author` varchar(64) NOT NULL,
  `UpdateTime` datetime NOT NULL,
  PRIMARY KEY (`Term`),
  KEY `Author` (`Author`),
  CONSTRAINT `terms_ibfk_1` FOREIGN KEY (`Author`) REFERENCES `Users` (`Username`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.


-- Dumping structure for table rter.UserDirections
DROP TABLE IF EXISTS `UserDirections`;
CREATE TABLE IF NOT EXISTS `UserDirections` (
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

-- Data exporting was unselected.


-- Dumping structure for table rter.Users
DROP TABLE IF EXISTS `Users`;
CREATE TABLE IF NOT EXISTS `Users` (
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

-- Data exporting was unselected.


-- Dumping structure for table rter.Users_Bkp
DROP TABLE IF EXISTS `Users_Bkp`;
CREATE TABLE IF NOT EXISTS `Users_Bkp` (
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

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
