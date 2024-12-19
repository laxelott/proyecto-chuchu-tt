USE Chuchu;
UPDATE Driver SET token = '5' WHERE idDriver = 8;
UPDATE Vehicle SET driverToken = '5' WHERE idVehicle = 40005;
CALL testDriverData('5');