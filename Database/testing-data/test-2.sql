USE Chuchu;
UPDATE Driver SET token = '2' WHERE idDriver = 5;
UPDATE Vehicle SET driverToken = '2' WHERE idVehicle = 40002;
CALL testDriverData('2');