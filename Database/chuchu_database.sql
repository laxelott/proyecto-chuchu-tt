DROP DATABASE IF EXISTS chuchu;
CREATE DATABASE chuchu;
USE chuchu;

CREATE TABLE transportation(
    idTransportation INT PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE line (
    idLine INT PRIMARY KEY AUTO_INCREMENT,
    idTransportation INT NOT NULL,
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(200),

    FOREIGN KEY (idTransportation) REFERENCES transportation(idTransportation)
);

CREATE TABLE stop (
    idStop INT PRIMARY KEY AUTO_INCREMENT,
    idLine INT NOT NULL, 
    name VARCHAR(200),
    coordsX DOUBLE NOT NULL,
    coordsY DOUBLE NOT NULL,
    idNext INT,

    FOREIGN KEY (idLine) REFERENCES line(idLine)
);

CREATE TABLE incident (
    idIncident INT PRIMARY KEY AUTO_INCREMENT,
    idLine INT NOT NULL,
    description VARCHAR(200),
    coordsX DOUBLE NOT NULL,
    coordsY DOUBLE NOT NULL,

    FOREIGN KEY (idLine) REFERENCES line(idLine)
);

CREATE TABLE vehicle (
    idVehicle INT PRIMARY KEY AUTO_INCREMENT,
    idTransportation INT NOT NULL,
    identifier VARCHAR(200) NOT NULL,

    FOREIGN KEY (idTransportation) REFERENCES transportation(idTransportation)
);

CREATE TABLE userType (
    idUserType INT PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE user (
    idUser INT PRIMARY KEY AUTO_INCREMENT,
    idUserType INT NOT NULL,
    name VARCHAR(200) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(200) NOT NULL,
    salt VARCHAR(35) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(100) NOT NULL,
    curp VARCHAR(50) NOT NULL UNIQUE,

    FOREIGN KEY (idUserType) REFERENCES userType(idUserType)
);

CREATE TABLE user_vehicle (
    idUserVehicle INT PRIMARY KEY AUTO_INCREMENT,
    idUser INT NOT NULL,
    idVehicle INT NOT NULL,

    FOREIGN KEY (idVehicle) REFERENCES vehicle(idVehicle),
    FOREIGN KEY (idUser) REFERENCES user(idUser)
)