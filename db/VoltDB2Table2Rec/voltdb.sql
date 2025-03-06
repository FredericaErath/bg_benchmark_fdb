CREATE TABLE CONFIRMEDFRIENDSHIP(
	FRIEND1 INTEGER NOT NULL, 
	FRIEND2 INTEGER NOT NULL, 
	PRIMARY KEY (FRIEND1,FRIEND2)
);

CREATE TABLE PENDINGFRIENDSHIP(
	INVITERID INTEGER NOT NULL, 
	INVITEEID INTEGER NOT NULL, 
	PRIMARY KEY (INVITERID, INVITEEID)
);
					
CREATE TABLE MANIPULATION(
	MID INTEGER NOT NULL, 
	MODIFIERID INTEGER NOT NULL, 
	RID INTEGER NOT NULL, 
	CREATORID INTEGER NOT NULL, 
	TIME VARCHAR(200), 
	TYPE VARCHAR(200), 
	CONTENT VARCHAR(200), 
	PRIMARY KEY (MID,RID)
);

CREATE TABLE RESOURCES(
	RID INTEGER NOT NULL , 
	CREATORID INTEGER NOT NULL, 
	WALLUSERID INTEGER NOT NULL, 
	TYPE VARCHAR(200), 
	BODY VARCHAR(200), 
	DOC VARCHAR(200), 
	PRIMARY KEY (RID)
);

CREATE TABLE USERS(
	USERID INTEGER NOT NULL , 
	USERNAME VARCHAR(200), 
	PW VARCHAR(200), 
	FNAME VARCHAR(200), 
	LNAME VARCHAR(200), 
	GENDER VARCHAR(200),
	DOB VARCHAR(200), 
	JDATE VARCHAR(200), 
	LDATE VARCHAR(200), 
	ADDRESS VARCHAR(200),
	EMAIL VARCHAR(200), 
	TEL VARCHAR(200),
	--FRIENDSCOUNT INTEGER,
	--PENDINGFRIENDSCOUNT INTEGER,
	--RESOURCESCOUNT INTEGER, 
	PRIMARY KEY (USERID)
);

CREATE INDEX PENDFRIENDSHIP_INVITEEID ON PENDINGFRIENDSHIP (INVITEEID);
CREATE INDEX PENDFRIENDSHIP_INVITERID ON PENDINGFRIENDSHIP (INVITERID);
CREATE INDEX CONFFRIENDSHIP_FRIEND1 ON CONFIRMEDFRIENDSHIP (FRIEND1);
CREATE INDEX CONFFRIENDSHIP_FRIEND2 ON CONFIRMEDFRIENDSHIP (FRIEND2);
CREATE INDEX MANIPULATION_RID ON MANIPULATION (RID);
CREATE INDEX MANIPULATION_CREATORID ON MANIPULATION (CREATORID);
CREATE INDEX RESOURCES_WALLUSERID ON RESOURCES (WALLUSERID);
CREATE INDEX RESOURCE_CREATORID ON RESOURCES (CREATORID);

CREATE PROCEDURE FROM CLASS voltdbbg.AcceptFriendship;
CREATE PROCEDURE FROM CLASS voltdbbg.CreateFriendship;
CREATE PROCEDURE FROM CLASS voltdbbg.ListFriends;
CREATE PROCEDURE FROM CLASS voltdbbg.CountFriends;
CREATE PROCEDURE FROM CLASS voltdbbg.CountPendingFriends;
CREATE PROCEDURE FROM CLASS voltdbbg.CountResources;
CREATE PROCEDURE FROM CLASS voltdbbg.GetUserDetails;
CREATE PROCEDURE FROM CLASS voltdbbg.GetFriendRequests;
CREATE PROCEDURE FROM CLASS voltdbbg.RejectFriend;
CREATE PROCEDURE FROM CLASS voltdbbg.InviteFriend;
CREATE PROCEDURE FROM CLASS voltdbbg.GetTopKResources;
CREATE PROCEDURE FROM CLASS voltdbbg.GetCreatedResources;
CREATE PROCEDURE FROM CLASS voltdbbg.GetCommentOnResource;
CREATE PROCEDURE FROM CLASS voltdbbg.PostCommentOnResource;
CREATE PROCEDURE FROM CLASS voltdbbg.DelCommentOnResource;
CREATE PROCEDURE FROM CLASS voltdbbg.ThawFriendship;
CREATE PROCEDURE FROM CLASS voltdbbg.GetPendingFriends;
CREATE PROCEDURE FROM CLASS voltdbbg.GetConfirmedFriends;