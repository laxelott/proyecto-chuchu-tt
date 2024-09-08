DROP DATABASE IF EXISTS chuchu;
CREATE DATABASE chuchu;
USE chuchu;

CREATE TABLE transportation(
    idTransportation INT PRIMARY KEY,
    name VARCHAR(200)
);

CREATE TABLE line (
    idLine INT PRIMARY KEY,
    idTransportation INT NOT NULL,
    name VARCHAR(200),
    description VARCHAR(200),

    FOREIGN KEY (idTransportation) REFERENCES transportation(idTransportation)
);

CREATE TABLE stop (
    idStop INT PRIMARY KEY,
    idLine INT NOT NULL, 
    name VARCHAR(200),
    coordsX DOUBLE,
    coordsY DOUBLE,
    idNext INT,

    FOREIGN KEY (idLine) REFERENCES line(idLine)
);

CREATE TABLE incident (
    idIncident INT PRIMARY KEY,
    idLine INT NOT NULL,
    description VARCHAR(200),
    coordsX DOUBLE,
    coordsY DOUBLE,

    FOREIGN KEY (idLine) REFERENCES line(idLine)
);

CREATE TABLE vehicle (
    idVehicle INT PRIMARY KEY,
    idTransportation INT NOT NULL,
    identifier VARCHAR(200),

    FOREIGN KEY (idTransportation) REFERENCES transportation(idTransportation)
);

CREATE TABLE userType (
    idUserType INT PRIMARY KEY,
    name VARCHAR(200)
);

CREATE TABLE user (
    idUser INT PRIMARY KEY,
    idUserType INT NOT NULL,
    name VARCHAR(200),
    username VARCHAR(50),
    password VARCHAR(200),
    salt VARCHAR(35),
    phone VARCHAR(30),
    email VARCHAR(100),
    curp VARCHAR(50),

    FOREIGN KEY (idUserType) REFERENCES userType(idUserType)
);

CREATE TABLE user_vehicle (
    idUserVehicle INT PRIMARY KEY,
    idUser INT NOT NULL,
    idVehicle INT NOT NULL,

    FOREIGN KEY (idVehicle) REFERENCES vehicle(idVehicle),
    FOREIGN KEY (idUser) REFERENCES user(idUser)
)