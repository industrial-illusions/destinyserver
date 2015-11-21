
CREATE TABLE IF NOT EXISTS `pn_db_fishing` (
  `id` int(2) NOT NULL AUTO_INCREMENT,
  `pokemon` varchar(32) NOT NULL,
  `experience` int(4) NOT NULL,
  `levelreq` int(2) NOT NULL,
  `rodreq` int(2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=32 ;

--
-- Dumping data for table `pn_db_fishing`
--

INSERT INTO `pn_db_fishing` (`id`, `pokemon`, `experience`, `levelreq`, `rodreq`) VALUES
(1, 'Magikarp', 35, 0, 0),
(2, 'Poliwag', 45, 0, 0),
(3, 'Goldeen', 50, 15, 15),
(4, 'Grimer', 65, 15, 15),
(5, 'Finneon', 65, 15, 15),
(6, 'Chinchou', 75, 20, 15),
(7, 'Remoraid', 100, 20, 15),
(8, 'Qwilfish', 100, 25, 15),
(9, 'Carvanha', 125, 30, 15),
(10, 'Shelder', 125, 30, 15),
(11, 'Clamperl', 125, 30, 15),
(12, 'Barboach', 130, 35, 15),
(13, 'Corphish', 130, 35, 15),
(14, 'Relicanth', 150, 40, 15),
(15, 'Wailmer', 175, 45, 15),
(16, 'Lumineon', 185, 50, 50),
(17, 'Feebas', 200, 50, 50),
(18, 'Crawdaunt', 225, 55, 50),
(19, 'Shellder', 225, 55, 50),
(20, 'Poliwhirl', 250, 60, 50),
(21, 'Whiscash', 250, 60, 50),
(22, 'Lanturn', 300, 65, 50),
(23, 'Sharpedo', 300, 65, 50),
(24, 'Corsola', 300, 65, 50),
(25, 'Horsea', 400, 70, 75),
(26, 'Staryu', 400, 70, 75),
(27, 'Wailord', 500, 75, 75),
(28, 'Octillery', 575, 80, 75),
(29, 'Gyarados', 650, 85, 75),
(30, 'Dratini', 750, 90, 75),
(31, 'Dragonair', 1000, 99, 75);