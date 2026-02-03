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
-- Table structure for table `client_holdings`
--

DROP TABLE IF EXISTS `client_holdings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_holdings` (
  `holding_id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` bigint NOT NULL,
  `asset_id` bigint NOT NULL,
  `quantity` decimal(38,2) DEFAULT NULL,
  `avg_buy_price` decimal(38,2) DEFAULT NULL,
  `buy_date` date NOT NULL,
  PRIMARY KEY (`holding_id`),
  UNIQUE KEY `client_id` (`client_id`,`asset_id`),
  KEY `asset_id` (`asset_id`),
  CONSTRAINT `client_holdings_ibfk_1` FOREIGN KEY (`client_id`) REFERENCES `clients` (`client_id`) ON DELETE CASCADE,
  CONSTRAINT `client_holdings_ibfk_2` FOREIGN KEY (`asset_id`) REFERENCES `assets` (`asset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client_holdings`
--

LOCK TABLES `client_holdings` WRITE;
/*!40000 ALTER TABLE `client_holdings` DISABLE KEYS */;
INSERT INTO `client_holdings` VALUES (10,5,11,10.00,58000.00,'2023-11-01'),(11,6,2,30.00,3400.00,'2023-12-10'),(12,7,3,75.00,1600.00,'2023-08-30'),(14,8,1,20.00,1200.00,'2026-02-03');
/*!40000 ALTER TABLE `client_holdings` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-03 10:53:28
