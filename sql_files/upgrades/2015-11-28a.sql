truncate `pn_vip`;
ALTER TABLE  `pn_members` CHANGE  `adminLevel`  `adminLevel` INT( 1 ) NULL DEFAULT NULL;
ALTER TABLE  `pn_members` CHANGE  `lastLanguageUsed`  `lastLanguageUsed` TINYINT( 1 ) NULL DEFAULT NULL;
ALTER TABLE  `pn_members` CHANGE  `sprite`  `sprite` TINYINT( 1 ) NULL DEFAULT NULL;
