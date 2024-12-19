USE Chuchu;
UPDATE Driver SET token = '3' WHERE idDriver = 6;
UPDATE Vehicle SET driverToken = '3' WHERE idVehicle = 40003;
CALL testDriverData('3');