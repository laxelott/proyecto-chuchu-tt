DROP DATABASE IF EXISTS chuchu;
CREATE DATABASE chuchu;
USE chuchu;

CREATE TABLE transporte (
    idTransporte INT PRIMARY KEY,
    nombre VARCHAR(200)
);

CREATE TABLE ruta (
    idRuta INT PRIMARY KEY,
    idTransporte INT NOT NULL,
    nombre VARCHAR(200),

    FOREIGN KEY (idTransporte) REFERENCES transporte(idTransporte)
);

CREATE TABLE parada (
    idParada INT PRIMARY KEY,
    idRuta INT NOT NULL, 
    nombre VARCHAR(200),
    coordsX DOUBLE,
    coordsY DOUBLE,
    orden INT NOT NULL,

    FOREIGN KEY (idRuta) REFERENCES ruta(idRuta)
);

CREATE TABLE incidencia (
    idIncidencia INT PRIMARY KEY,
    idRuta INT NOT NULL,
    descripcion VARCHAR(200),
    coordsX DOUBLE,
    coordsY DOUBLE,

    FOREIGN KEY (idRuta) REFERENCES ruta(idRuta)
);

CREATE TABLE vehiculo (
    idVehiculo INT PRIMARY KEY,
    idTransporte INT NOT NULL,
    matricula VARCHAR(200),

    FOREIGN KEY (idTransporte) REFERENCES transporte(idTransporte)
);

CREATE TABLE tipoUsuario (
    idTipoUsuario INT PRIMARY KEY,
    nombre VARCHAR(200)
);

CREATE TABLE usuario (
    idUsuario INT PRIMARY KEY,
    idVehiculo INT NOT NULL,
    idTipoUsuario INT NOT NULL,
    nombre VARCHAR(200),
    nomUsuario VARCHAR(200),
    telefono VARCHAR(30),
    correo VARCHAR(200),
    curp VARCHAR(200),

    FOREIGN KEY (idVehiculo) REFERENCES vehiculo(idVehiculo),
    FOREIGN KEY (idTipoUsuario) REFERENCES tipoUsuario(idTipoUsuario)
);