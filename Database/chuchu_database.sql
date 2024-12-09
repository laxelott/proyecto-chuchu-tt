/****** Object:  Database `Chuchu` ******/
DROP DATABASE IF EXISTS Chuchu;
CREATE DATABASE `Chuchu`;
USE `Chuchu`;

SET foreign_key_checks = 0;

/****** Object:  Table `Admin_Transport` ******/
CREATE TABLE `Admin_Transport`(
    `idAdminTransport` int NOT NULL AUTO_INCREMENT,
    `idTransport` int NOT NULL,
    `idAdmin` int NOT NULL,
    CONSTRAINT `PK_Admin_Transport` PRIMARY KEY (`idAdminTransport` ASC),
    CONSTRAINT `FK_Admin_Transport_Admin` FOREIGN KEY (`idAdmin`)
        REFERENCES `Admin` (`idAdmin`),
    CONSTRAINT `FK_Admin_Transport_Transport` FOREIGN KEY (`idTransport`)
        REFERENCES `Transport` (`idTransport`)
);

/****** Object:  Table `Admin` ******/
CREATE TABLE `Admin`(
    `idAdmin` int NOT NULL AUTO_INCREMENT,
    `username` varchar(255) NOT NULL UNIQUE,
    `password` varchar(100) NOT NULL,
    `salt` varchar(40) NOT NULL UNIQUE,
    `token` varchar(40) UNIQUE,
    CONSTRAINT `PK_Admin` PRIMARY KEY (`idAdmin` ASC)
);

/****** Object:  Table `Driver` ******/
CREATE TABLE `Driver`(
    `idDriver` int NOT NULL AUTO_INCREMENT,
    `curp` varchar(50) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `surnameP` varchar(255) NULL,
    `surnameM` varchar(255) NULL,
    `username` varchar(50) NOT NULL UNIQUE,
    `password` varchar(100) NOT NULL,
    `salt` varchar(40) NOT NULL UNIQUE,
    `phone` varchar(20) NOT NULL,
    `active` tinyint NOT NULL,
    `token` varchar(40) UNIQUE,
    `requiresReset` boolean NOT NULL DEFAULT 0,
    CONSTRAINT `PK_Driver` PRIMARY KEY (`idDriver` ASC)
);

/****** Object:  Table `Driver_Vehicle` ******/
CREATE TABLE `Driver_Vehicle`(
    `idDriverVehicle` int NOT NULL AUTO_INCREMENT,
    `idDriver` int NULL,
    `idVehicle` int NULL,
    CONSTRAINT `PK_Driver_Vehicle` PRIMARY KEY (`idDriverVehicle` ASC),
    CONSTRAINT `FK_Driver_Vehicle_Driver` FOREIGN KEY (`idDriver`)
        REFERENCES `Driver` (`idDriver`),
    CONSTRAINT `FK_Driver_Vehicle_Vehicle` FOREIGN KEY (`idVehicle`)
        REFERENCES `Vehicle` (`idVehicle`)
);

/****** Object:  Table `Stop` ******/
CREATE TABLE `Stop`(
    `idStop` int NOT NULL AUTO_INCREMENT,
    `idRoute` int NOT NULL,
    `name` varchar(255) NOT NULL,
    `lat` double NOT NULL,
    `lon` double NOT NULL,
    `distanceTo` float NOT NULL DEFAULT 0,
    `iconB64` TEXT,
    `idNext` int,
    CONSTRAINT `PK_Stop` PRIMARY KEY (`idStop` ASC),
    CONSTRAINT `FK_Stop_Route` FOREIGN KEY (`idRoute`)
        REFERENCES `Route` (`idRoute`)
);

/****** Object:  Table `Incident` ******/
CREATE TABLE `Incident`(
    `idIncident` int NOT NULL AUTO_INCREMENT,
    `idIncidentType` int NOT NULL,
    `idRoute` int NOT NULL,
    `description` varchar(255) NOT NULL,
    `lat` double NOT NULL,
    `lon` double NOT NULL,
    CONSTRAINT `PK_Incident` PRIMARY KEY (`idIncident` ASC),
    CONSTRAINT `FK_Incident_Route` FOREIGN KEY (`idRoute`)
        REFERENCES `Route` (`idRoute`),
    CONSTRAINT `FK_Incident_IncidentType` FOREIGN KEY (`idIncidentType`)
        REFERENCES `IncidentType` (`idIncidentType`)
);

/****** Object:  Table `IncidentType` ******/
CREATE TABLE `IncidentType`(
    `idIncidentType` int NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    CONSTRAINT `PK_IncidentType` PRIMARY KEY (`idIncidentType` ASC)
);

/****** Object:  Table `Driver_Incident` ******/
CREATE TABLE `Driver_Incident`(
    `idDriverIncident` int NOT NULL AUTO_INCREMENT,
    `idIncident` int NOT NULL,
    `idDriver` int NOT NULL,
    CONSTRAINT `PK_Driver_Incident` PRIMARY KEY (`idDriverIncident` ASC),
    CONSTRAINT `FK_Driver_Incident_Driver` FOREIGN KEY (`idDriver`)
        REFERENCES `Driver` (`idDriver`),
    CONSTRAINT `FK_Driver_Incident_Incident` FOREIGN KEY (`idIncident`)
        REFERENCES `Incident` (`idIncident`)
);

/****** Object:  Table `Route` ******/
CREATE TABLE `Route`(
    `idRoute` int NOT NULL AUTO_INCREMENT,
    `idTransport` int NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(255) NOT NULL,
    `color` varchar(10) NOT NULL,
    `iconB64` TEXT,
    `idTerminal` int NOT NULL, 
    CONSTRAINT `PK_Route` PRIMARY KEY (`idRoute` ASC),
    CONSTRAINT `FK_Route_Transport` FOREIGN KEY (`idTransport`)
        REFERENCES `Transport` (`idTransport`)
);

/****** Object:  Table `Transport` ******/
CREATE TABLE `Transport`(
    `idTransport` int NOT NULL AUTO_INCREMENT,
    `name` varchar(255) NOT NULL UNIQUE,
    `iconB64` TEXT,
    CONSTRAINT `PK_Transport` PRIMARY KEY (`idTransport` ASC)
);

/****** Object:  Table `Last_Location` ******/
CREATE TABLE `Last_Location`(
    `idLocation` int NOT NULL AUTO_INCREMENT,
    `idVehicle` int NOT NULL,
    `idLastStop` int NOT NULL,
    `lat` double NOT NULL,
    `lon` double NOT NULL,
    CONSTRAINT `PK_Last_Location` PRIMARY KEY (`idLocation` ASC),
    CONSTRAINT `FK_Last_Location_Vehicle` FOREIGN KEY (`idVehicle`)
        REFERENCES `Vehicle` (`idVehicle`),
    CONSTRAINT `FK_Last_Location_Stop` FOREIGN KEY (`idLastStop`)
        REFERENCES `Stop` (`idStop`)
);

/****** Object:  Table `PK_VehicleData` ******/
CREATE TABLE `VehicleData`(
    `idVehicleSpeed` int NOT NULL AUTO_INCREMENT,
    `idVehicle` int NOT NULL,
    `direction` float NOT NULL,
    `distanceToStop` float NOT NULL,
    `avgSpeed` float NOT NULL,
    `inStop` int NOT NULL default 0,
    CONSTRAINT `PK_VehicleData` PRIMARY KEY (`idVehicleSpeed` ASC),
    CONSTRAINT `FK_VehicleData_Vehicle` FOREIGN KEY (`idVehicle`)
        REFERENCES `Vehicle` (`idVehicle`)
);

/****** Object:  Table `Vehicle` ******/
CREATE TABLE `Vehicle`(
    `idVehicle` int NOT NULL AUTO_INCREMENT,
    `identifier` varchar(255) NOT NULL UNIQUE,
    `driverToken` varchar(40),
    `disabled` int NOT NULL DEFAULT 0,
    CONSTRAINT `PK_Vehicle` PRIMARY KEY (`idVehicle` ASC)
);

/****** Object:  Table `Vehicle_Route` ******/
CREATE TABLE `Vehicle_Route`(
    `idVehicleRoute` int NOT NULL AUTO_INCREMENT,
    `idRoute` int NOT NULL,
    `idVehicle` int NOT NULL,
    CONSTRAINT `PK_Vehicle_Route` PRIMARY KEY (`idVehicleRoute` ASC),
    CONSTRAINT `FK_Vehicle_Route_Route` FOREIGN KEY (`idRoute`)
        REFERENCES `Route` (`idRoute`),
    CONSTRAINT `FK_Vehicle_Route_Vehicle` FOREIGN KEY (`idVehicle`)
        REFERENCES `Vehicle` (`idVehicle`)
);

/****** Object:  Table `Waypoint` ******/
CREATE TABLE `Waypoint`(
    `idWaypoint` int NOT NULL AUTO_INCREMENT,
    `lon` double NOT NULL,
    `lat` double NOT NULL,
    `idStop` int NOT NULL,
    CONSTRAINT `Waypoint` PRIMARY KEY (`idWaypoint` ASC),
    CONSTRAINT `FK_Waypoint_Stop` FOREIGN KEY (`idStop`)
        REFERENCES `Stop` (`idStop`)
);

/****** Object:  Table `CancelReason` ******/
CREATE TABLE `CancelReason`(
    `idCancelReason` int NOT NULL AUTO_INCREMENT,
    `idDriver` int NOT NULL,
    `time` DATETIME NOT NULL,
    `reason` varchar(200) NOT NULL,
    CONSTRAINT `CancelReason` PRIMARY KEY (`idCancelReason` ASC),
    CONSTRAINT `FK_CancelReason_Driver` FOREIGN KEY (`idDriver`)
        REFERENCES `Driver` (`idDriver`)

);


SET foreign_key_checks = 0;