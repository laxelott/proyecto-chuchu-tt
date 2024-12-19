USE Chuchu;
UPDATE Driver SET token = '1' WHERE idDriver = 4;
UPDATE Vehicle SET driverToken = '1' WHERE idVehicle = 40001;
CALL testDriverData('1');