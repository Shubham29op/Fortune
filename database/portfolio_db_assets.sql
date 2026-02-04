CREATE DATABASE  IF NOT EXISTS `portfolio_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `portfolio_db`;
-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: localhost    Database: portfolio_db
-- ------------------------------------------------------
-- Server version	8.0.41

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
-- Table structure for table `assets`
--

DROP TABLE IF EXISTS `assets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assets` (
  `asset_id` bigint NOT NULL AUTO_INCREMENT,
  `symbol` varchar(255) NOT NULL,
  `asset_name` varchar(255) NOT NULL,
  `asset_category` enum('NSE','MF','COMMODITY') NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT '1',
  `category` enum('COMMODITY','MF','NSE') NOT NULL,
  PRIMARY KEY (`asset_id`),
  UNIQUE KEY `symbol` (`symbol`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assets`
--

LOCK TABLES `assets` WRITE;
/*!40000 ALTER TABLE `assets` DISABLE KEYS */;
INSERT INTO `assets` VALUES (1,'GOLD','Gold 24k','COMMODITY',NULL,1,'COMMODITY'),(2,'SILVER','Silver','COMMODITY',NULL,1,'COMMODITY'),(3,'COPPER','Copper','COMMODITY',NULL,1,'COMMODITY'),(4,'RELIANCE','Reliance Industries','NSE','Oil, Gas, and Telecom giant',1,'NSE'),(5,'TCS','Tata Consultancy Services','NSE','IT Services',1,'NSE'),(6,'HDFCBANK','HDFC Bank','NSE','Banking and Finance',1,'NSE'),(7,'INFY','Infosys','NSE','IT Services',1,'NSE'),(8,'ICICIBANK','ICICI Bank','NSE','Banking and Finance',1,'NSE'),(9,'SBI_BLUECHIP','SBI Bluechip Fund','NSE','Large Cap Fund',1,'MF'),(10,'HDFC_BALANCED','HDFC Balanced Advantage','NSE','Hybrid Fund',1,'MF'),(11,'AXIS_LONGTERM','Axis Long Term Equity','NSE','ELSS Tax Saver',1,'MF'),(12,'ICICI_TECH','ICICI Prudential Technology','NSE','Sectoral IT Fund',1,'MF'),(13,'MOTILAL_MIDCAP','Motilal Oswal Midcap','NSE','Mid Cap Fund',1,'MF');
/*!40000 ALTER TABLE `assets` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-03 10:53:27
