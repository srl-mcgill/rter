-- MySQL rter v2
-- ===========
-- Run these commands to setup the MySQL databases for the rter v2 project

SET foreign_key_checks = 0;

DROP TABLE IF EXISTS Roles;
CREATE TABLE IF NOT EXISTS Roles (
	Title VARCHAR(64) NOT NULL,
	Permissions INT NOT NULL DEFAULT 0,

	PRIMARY KEY(Title)
);

DROP TABLE IF EXISTS Users;
CREATE TABLE IF NOT EXISTS Users (
	Username VARCHAR(64) NOT NULL,
	Password CHAR(128) NOT NULL,
	Salt CHAR(128) NOT NULL,

	Role VARCHAR(64) NOT NULL DEFAULT "public",
	TrustLevel INT NOT NULL DEFAULT 0,

	CreateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

	PRIMARY KEY(Username),
	UNIQUE KEY(Username),
	FOREIGN KEY(Role) REFERENCES Roles (Title) ON UPDATE CASCADE
);

DROP TABLE IF EXISTS UserDirections;
CREATE TABLE IF NOT EXISTS UserDirections (
	Username VARCHAR(64) NOT NULL,
	LockUsername VARCHAR(64) NOT NULL,
	Command VARCHAR(64) NOT NULL DEFAULT "none",

	Heading DECIMAL(9, 6) NOT NULL DEFAULT 0,
	Lat DECIMAL(9, 6) NOT NULL DEFAULT 0,
	Lng DECIMAL(9, 6) NOT NULL DEFAULT 0,

	UpdateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	PRIMARY KEY(Username),
	FOREIGN KEY(Username) REFERENCES Users (Username) ON UPDATE CASCADE ON DELETE CASCADE
);

DROP TABLE IF EXISTS Items;
CREATE TABLE IF NOT EXISTS Items (
	ID INT NOT NULL AUTO_INCREMENT,
	Type VARCHAR(64) NOT NULL,
	Author VARCHAR(64) NOT NULL,

	ThumbnailURI VARCHAR(2048) NOT NULL DEFAULT "",
	ContentURI VARCHAR(2048) NOT NULL DEFAULT "",
	UploadURI VARCHAR(2048) NOT NULL DEFAULT "",

	HasHeading TINYINT(1) NOT NULL DEFAULT 0,
	Heading DECIMAL(9, 6) NOT NULL DEFAULT 0,

	HasGeo TINYINT(1) NOT NULL DEFAULT 0,
	Lat DECIMAL(9, 6) NOT NULL DEFAULT 0,
	Lng DECIMAL(9, 6) NOT NULL DEFAULT 0,

	Live TINYINT(1) NOT	NULL DEFAULT 0,
	StartTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	StopTime DATETIME NOT NULL,

	PRIMARY KEY(ID),
	FOREIGN KEY(Author) REFERENCES Users (Username) ON UPDATE CASCADE
);

DROP TABLE IF EXISTS ItemComments;
CREATE TABLE IF NOT EXISTS ItemComments (
	ID INT NOT NULL AUTO_INCREMENT,
	ItemID INT NOT NULL,
	Author VARCHAR(64) NOT NULL,

	Body TEXT NOT NULL,

	UpdateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	PRIMARY KEY(ID),
	FOREIGN KEY(ItemID) REFERENCES Items (ID) ON UPDATE CASCADE ON DELETE CASCADE,
	FOREIGN KEY(Author) REFERENCES Users (Username) ON UPDATE CASCADE
);

DROP TABLE IF EXISTS Terms;
CREATE TABLE IF NOT EXISTS Terms (
	Term VARCHAR(64) NOT NULL,

	Automated TINYINT(1) NOT NULL DEFAULT 0,
	Author VARCHAR(64) NOT NULL,

	UpdateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP  ON UPDATE CURRENT_TIMESTAMP,

	PRIMARY KEY(Term),
	FOREIGN KEY(Author) REFERENCES Users (Username) ON UPDATE CASCADE
);

DROP TABLE IF EXISTS TermRelationships;
CREATE TABLE IF NOT EXISTS TermRelationships (
	Term VARCHAR(64) NOT NULL,
	ItemID INT NOT NULL,
	PRIMARY KEY(Term, ItemID),
	FOREIGN KEY(Term) REFERENCES Terms (Term) ON UPDATE CASCADE ON DELETE CASCADE,
	FOREIGN KEY(ItemID) REFERENCES Items (ID) ON UPDATE CASCADE ON DELETE CASCADE
);

DROP TABLE IF EXISTS TermRankings;
CREATE TABLE IF NOT EXISTS TermRankings (
	Term VARCHAR(64) NOT NULL,
	Ranking TEXT NOT NULL,
	
	UpdateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	PRIMARY KEY(Term),
	FOREIGN KEY(Term) REFERENCES Terms (Term) ON UPDATE CASCADE ON DELETE CASCADE
);

INSERT INTO Roles (Title, Permissions) VALUES ("public", 1), ("observer", 1), ("responder", 3), ("editor", 7), ("admin", 15);

INSERT INTO Users (Username, Password, Salt, Role, TrustLevel, CreateTime) VALUES ("anonymous", "", "", "public", 0, "2013-03-19 00:00:00"), ("admin", "", "", "admin", 0, "2013-03-19 00:00:00");
INSERT INTO UserDirections (Username) VALUES ("anonymous"), ("admin");

INSERT INTO Terms (Term, Automated, Author, UpdateTime) VALUES ("all", 1, "admin", "2013-03-19 00:00:00"), ("test", 0, "anonymous", "2013-03-19 00:00:00");
INSERT INTO TermRankings (Term, Ranking, UpdateTime) VALUES ("all", "",  "2013-03-19 00:00:00"), ("test", "",  "2013-03-19 00:00:00")

-- DROP TABLE IF EXISTS TaxonomyRankingsArchive;
-- CREATE TABLE IF NOT EXISTS TaxonomyRankingsArchive (
-- 	ID INT NOT NULL,
-- 	TaxonomyRankingID INT NOT NULL,
-- 	Ranking TEXT NOT NULL,

-- 	TaxonomyID INT NOT NULL,
	
-- 	UpdateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

-- 	PRIMARY KEY(ID),
-- 	FOREIGN KEY(TaxonomyRankingID) REFERENCES TaxonomyRankings (ID) ON UPDATE CASCADE,
-- 	FOREIGN KEY(TaxonomyID) REFERENCES Taxonomy (ID) ON UPDATE CASCADE
-- );

SET foreign_key_checks = 1;