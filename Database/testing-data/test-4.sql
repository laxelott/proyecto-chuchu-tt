USE Chuchu;
UPDATE Driver SET token = '4' WHERE idDriver = 7;
UPDATE Vehicle SET driverToken = '4' WHERE idVehicle = 40004;
CALL testDriverData('4');