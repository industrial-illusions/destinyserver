CREATE TABLE IF NOT EXISTS `pn_log_errors` (
  `error_id` int(6) NOT NULL AUTO_INCREMENT,
  `message` text NOT NULL,
  `action` text NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`error_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;


CREATE TABLE IF NOT EXISTS `pn_log_info` (
  `log_id` int(6) NOT NULL AUTO_INCREMENT,
  `message` text NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;


CREATE TABLE IF NOT EXISTS `pn_log_sqlupgrades` (
  `upgrade_id` int(3) NOT NULL AUTO_INCREMENT,
  `filename` varchar(32) NOT NULL DEFAULT '0',
  `timestamp` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`upgrade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;