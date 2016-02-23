/*
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
CREATE DATABASE IF NOT EXISTS GEO;

USE GEO;

DROP TABLE IF EXISTS `blocks`;
CREATE TABLE  `blocks` ( `startIPNum` int(10) unsigned NOT NULL,`endIPNum` int(10) unsigned NOT NULL,`locID`
int(10) unsigned NOT NULL, PRIMARY KEY  (`startIPNum`,`endIPNum`) )
ENGINE=MyISAM DEFAULT CHARSET=latin1 PACK_KEYS=1 DELAY_KEY_WRITE=1;

DROP TABLE IF EXISTS `location`;
CREATE TABLE  `location` (`locID` int(10) unsigned NOT NULL,`country` char(2) default NULL,`region` char(2)
 default NULL,`city` varchar(45) default NULL,`postalCode` char(7) default NULL,`latitude` double default
NULL,`longitude` double default NULL,`dmaCode` char(3) default NULL,`areaCode` char(3) default NULL,PRIMARY KEY
  (`locID`),KEY `Index_Country` (`country`) ) ENGINE=MyISAM DEFAULT CHARSET=latin1 ROW_FORMAT=FIXED;

load data infile '/var/lib/mysql-files/GeoLiteCity-Blocks.csv'  into table `blocks`  fields terminated by ',' optionally enclosed by '"'  lines terminated by '\n' ignore 2 lines;
load data infile '/var/lib/mysql-files/GeoLiteCity-Location.csv'  into table `location`  fields terminated by ',' optionally enclosed by '"'  lines terminated by '\n' ignore 2 lines;


DELIMITER $$
DROP FUNCTION IF EXISTS `IPTOLOCID` $$
CREATE FUNCTION `IPTOLOCID`( ip VARCHAR(15)) RETURNS int(10) unsigned
  BEGIN
    DECLARE ipn INTEGER UNSIGNED;
    DECLARE locID_var INTEGER;
    IF ip LIKE '192.168.%' OR ip LIKE '10.%' THEN RETURN 0;
    END IF;
    SET ipn = INET_ATON(ip);
    SELECT locID INTO locID_var FROM `blocks` INNER JOIN (SELECT MAX(startIPNum) AS start FROM `blocks` WHERE startIPNum <= ipn) AS s ON (startIPNum = s.start) WHERE endIPNum >= ipn;
    RETURN locID_var;
  END
$$
DELIMITER ;
