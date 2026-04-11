-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: quran_db
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `surahs`
--

DROP TABLE IF EXISTS `surahs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `surahs` (
  `surah_id` int NOT NULL,
  `name_arabic` varchar(255) DEFAULT NULL,
  `name_simple` varchar(255) DEFAULT NULL,
  `name_english` varchar(255) DEFAULT NULL,
  `revelation_place` varchar(20) DEFAULT NULL,
  `total_ayahs` int DEFAULT NULL,
  PRIMARY KEY (`surah_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `surahs`
--

LOCK TABLES `surahs` WRITE;
/*!40000 ALTER TABLE `surahs` DISABLE KEYS */;
INSERT INTO `surahs` (`surah_id`, `name_arabic`, `name_simple`, `name_english`, `revelation_place`, `total_ayahs`) VALUES (1,'الفاتحة','Al-Fatihah','The Opener','Makkah',7),(2,'البقرة','Al-Baqarah','The Cow','Madinah',286),(3,'آل عمران','Ali \'Imran','Family of Imran','Madinah',200),(4,'النساء','An-Nisa','The Women','Madinah',176),(5,'المائدة','Al-Ma\'idah','The Table Spread','Madinah',120),(6,'الأنعام','Al-An\'am','The Cattle','Makkah',165),(7,'الأعراف','Al-A\'raf','The Heights','Makkah',206),(8,'الأنفال','Al-Anfal','The Spoils of War','Madinah',75),(9,'التوبة','At-Tawbah','The Repentance','Madinah',129),(10,'يونس','Yunus','Jonah','Makkah',109),(11,'هود','Hud','Hud','Makkah',123),(12,'يوسف','Yusuf','Joseph','Makkah',111),(13,'الرعد','Ar-Ra\'d','The Thunder','Madinah',43),(14,'ابراهيم','Ibrahim','Abraham','Makkah',52),(15,'الحجر','Al-Hijr','The Rocky Tract','Makkah',99),(16,'النحل','An-Nahl','The Bee','Makkah',128),(17,'الإسراء','Al-Isra','The Night Journey','Makkah',111),(18,'الكهف','Al-Kahf','The Cave','Makkah',110),(19,'مريم','Maryam','Mary','Makkah',98),(20,'طه','Taha','Ta-Ha','Makkah',135),(21,'الأنبياء','Al-Anbya','The Prophets','Makkah',112),(22,'الحج','Al-Hajj','The Pilgrimage','Madinah',78),(23,'المؤمنون','Al-Mu\'minun','The Believers','Makkah',118),(24,'النور','An-Nur','The Light','Madinah',64),(25,'الفرقان','Al-Furqan','The Criterion','Makkah',77),(26,'الشعراء','Ash-Shu\'ara','The Poets','Makkah',227),(27,'النمل','An-Naml','The Ant','Makkah',93),(28,'القصص','Al-Qasas','The Stories','Makkah',88),(29,'العنكبوت','Al-\'Ankabut','The Spider','Makkah',69),(30,'الروم','Ar-Rum','The Romans','Makkah',60),(31,'لقمان','Luqman','Luqman','Makkah',34),(32,'السجدة','As-Sajdah','The Prostration','Makkah',30),(33,'الأحزاب','Al-Ahzab','The Combined Forces','Madinah',73),(34,'سبإ','Saba','Sheba','Makkah',54),(35,'فاطر','Fatir','Originator','Makkah',45),(36,'يس','Ya-Sin','Ya Sin','Makkah',83),(37,'الصافات','As-Saffat','Those who set the Ranks','Makkah',182),(38,'ص','Sad','The Letter \"Saad\"','Makkah',88),(39,'الزمر','Az-Zumar','The Troops','Makkah',75),(40,'غافر','Ghafir','The Forgiver','Makkah',85),(41,'فصلت','Fussilat','Explained in Detail','Makkah',54),(42,'الشورى','Ash-Shuraa','The Consultation','Makkah',53),(43,'الزخرف','Az-Zukhruf','The Ornaments of Gold','Makkah',89),(44,'الدخان','Ad-Dukhan','The Smoke','Makkah',59),(45,'الجاثية','Al-Jathiyah','The Crouching','Makkah',37),(46,'الأحقاف','Al-Ahqaf','The Wind-Curved Sandhills','Makkah',35),(47,'محمد','Muhammad','Muhammad','Madinah',38),(48,'الفتح','Al-Fath','The Victory','Madinah',29),(49,'الحجرات','Al-Hujurat','The Rooms','Madinah',18),(50,'ق','Qaf','The Letter \"Qaf\"','Makkah',45),(51,'الذاريات','Adh-Dhariyat','The Winnowing Winds','Makkah',60),(52,'الطور','At-Tur','The Mount','Makkah',49),(53,'النجم','An-Najm','The Star','Makkah',62),(54,'القمر','Al-Qamar','The Moon','Makkah',55),(55,'الرحمن','Ar-Rahman','The Beneficent','Madinah',78),(56,'الواقعة','Al-Waqi\'ah','The Inevitable','Makkah',96),(57,'الحديد','Al-Hadid','The Iron','Madinah',29),(58,'المجادلة','Al-Mujadila','The Pleading Woman','Madinah',22),(59,'الحشر','Al-Hashr','The Exile','Madinah',24),(60,'الممتحنة','Al-Mumtahanah','She that is to be examined','Madinah',13),(61,'الصف','As-Saf','The Ranks','Madinah',14),(62,'الجمعة','Al-Jumu\'ah','The Congregation, Friday','Madinah',11),(63,'المنافقون','Al-Munafiqun','The Hypocrites','Madinah',11),(64,'التغابن','At-Taghabun','The Mutual Disillusion','Madinah',18),(65,'الطلاق','At-Talaq','The Divorce','Madinah',12),(66,'التحريم','At-Tahrim','The Prohibition','Madinah',12),(67,'الملك','Al-Mulk','The Sovereignty','Makkah',30),(68,'القلم','Al-Qalam','The Pen','Makkah',52),(69,'الحاقة','Al-Haqqah','The Reality','Makkah',52),(70,'المعارج','Al-Ma\'arij','The Ascending Stairways','Makkah',44),(71,'نوح','Nuh','Noah','Makkah',28),(72,'الجن','Al-Jinn','The Jinn','Makkah',28),(73,'المزمل','Al-Muzzammil','The Enshrouded One','Makkah',20),(74,'المدثر','Al-Muddaththir','The Cloaked One','Makkah',56),(75,'القيامة','Al-Qiyamah','The Resurrection','Makkah',40),(76,'الانسان','Al-Insan','The Man','Madinah',31),(77,'المرسلات','Al-Mursalat','The Emissaries','Makkah',50),(78,'النبإ','An-Naba','The Tidings','Makkah',40),(79,'النازعات','An-Nazi\'at','Those who drag forth','Makkah',46),(80,'عبس','\'Abasa','He Frowned','Makkah',42),(81,'التكوير','At-Takwir','The Overthrowing','Makkah',29),(82,'الإنفطار','Al-Infitar','The Cleaving','Makkah',19),(83,'المطففين','Al-Mutaffifin','The Defrauding','Makkah',36),(84,'الإنشقاق','Al-Inshiqaq','The Sundering','Makkah',25),(85,'البروج','Al-Buruj','The Mansions of the Stars','Makkah',22),(86,'الطارق','At-Tariq','The Nightcommer','Makkah',17),(87,'الأعلى','Al-A\'la','The Most High','Makkah',19),(88,'الغاشية','Al-Ghashiyah','The Overwhelming','Makkah',26),(89,'الفجر','Al-Fajr','The Dawn','Makkah',30),(90,'البلد','Al-Balad','The City','Makkah',20),(91,'الشمس','Ash-Shams','The Sun','Makkah',15),(92,'الليل','Al-Layl','The Night','Makkah',21),(93,'الضحى','Ad-Duhaa','The Morning Hours','Makkah',11),(94,'الشرح','Ash-Sharh','The Relief','Makkah',8),(95,'التين','At-Tin','The Fig','Makkah',8),(96,'العلق','Al-\'Alaq','The Clot','Makkah',19),(97,'القدر','Al-Qadr','The Power','Makkah',5),(98,'البينة','Al-Bayyinah','The Clear Proof','Madinah',8),(99,'الزلزلة','Az-Zalzalah','The Earthquake','Madinah',8),(100,'العاديات','Al-\'Adiyat','The Courser','Makkah',11),(101,'القارعة','Al-Qari\'ah','The Calamity','Makkah',11),(102,'التكاثر','At-Takathur','The Rivalry in world increase','Makkah',8),(103,'العصر','Al-\'Asr','The Declining Day','Makkah',3),(104,'الهمزة','Al-Humazah','The Traducer','Makkah',9),(105,'الفيل','Al-Fil','The Elephant','Makkah',5),(106,'قريش','Quraysh','Quraysh','Makkah',4),(107,'الماعون','Al-Ma\'un','The Small kindnesses','Makkah',7),(108,'الكوثر','Al-Kawthar','The Abundance','Makkah',3),(109,'الكافرون','Al-Kafirun','The Disbelievers','Makkah',6),(110,'النصر','An-Nasr','The Divine Support','Madinah',3),(111,'المسد','Al-Masad','The Palm Fiber','Makkah',5),(112,'الإخلاص','Al-Ikhlas','The Sincerity','Makkah',4),(113,'الفلق','Al-Falaq','The Daybreak','Makkah',5),(114,'الناس','An-Nas','Mankind','Makkah',6);
/*!40000 ALTER TABLE `surahs` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-02  0:49:11
