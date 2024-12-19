USE Chuchu;

DELIMITER $$


-- -----------------------------------------------------------------------------
  -- getHaversineDelta
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getHaversineDelta$$
CREATE PROCEDURE getHaversineDelta(
    x1 float,
    y1 float,
    x2 float,
    y2 float,
    OUT outHaversine FLOAT
)
BEGIN
    
    SET @R = 6371000;
    SET outHaversine = (
        @R * 2 * ASIN(SQRT(
                POWER(SIN(RADIANS(y2 - y1) / 2), 2) +
                COS(RADIANS(y1)) * COS(RADIANS(y2)) * 
                POWER(SIN(RADIANS(x2 - x1) / 2), 2)
            ))
    );

END$$


-- -----------------------------------------------------------------------------
  -- loginDriver
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS loginDriver$$
CREATE PROCEDURE loginDriver(
    `pUsername` varchar(100),
    `pPassword` varchar(255)
)
BEGIN

    DECLARE `login` int;
    DECLARE `newToken` varchar(40);
    CALL generateToken(newToken);

    IF (SELECT token FROM Driver d WHERE d.username = pUsername) IS NOT NULL THEN
        SET login = 2;
    ELSE
        SET login = (
            SELECT count(*) AS auth FROM Driver d
            WHERE d.username = pUsername
                AND d.password = pPassword
        );
        
        UPDATE Driver d
            SET d.token = newToken
            WHERE d.username = pUsername;

    END IF;
   
    IF login = 1 THEN
        UPDATE Driver d
            SET requiresReset = 0
        WHERE d.username = pUsername;

        SELECT login, newToken as token;
    ELSE
        SELECT login;
    END IF;

END$$


-- -----------------------------------------------------------------------------
  -- logoutDriver
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS logoutDriver$$
CREATE PROCEDURE logoutDriver(
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Driver d WHERE d.token = pToken) > 0 THEN
        UPDATE Driver d SET
            d.token = NULL WHERE d.token = pToken;

        SELECT 1 AS logout;
    ELSE
        SELECT 0 AS logout;
    END IF;

END$$


-- -----------------------------------------------------------------------------
  -- checkToken
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS checkToken$$
CREATE PROCEDURE checkToken(
    `pToken` varchar(255),
    `pUsername` varchar(100)
)
BEGIN

    DECLARE `login` int;
    DECLARE `newToken` varchar(40);
    CALL generateToken(newToken);

    SELECT NOT COUNT(*) AS error FROM Driver d
        WHERE d.username = pUsername AND d.token = pToken;

END$$


-- -----------------------------------------------------------------------------
  -- checkVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS checkVehicle$$
CREATE PROCEDURE checkVehicle(
    `pToken` varchar(255),
    `pIdentifier` varchar(100)
)
BEGIN

    DECLARE `login` int;
    DECLARE `newToken` varchar(40);
    CALL generateToken(newToken);

    SELECT NOT COUNT(*) AS error FROM Vehicle v
        WHERE v.identifier = pIdentifier AND v.driverToken = pToken;

END$$


-- -----------------------------------------------------------------------------
  -- getTransports
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getTransports$$
CREATE PROCEDURE getTransports()
BEGIN

    SELECT
        t.idTransport AS `id`,
        t.name as `name`,
        t.iconB64 as `icon`
    FROm Transport t
    ORDER BY name;

END$$

-- -----------------------------------------------------------------------------
  -- getRoutesFromTransport
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getRoutesFromTransport$$
CREATE PROCEDURE getRoutesFromTransport(
    `pIdTransport` int
)
BEGIN

    SELECT
        r.idRoute as `id`,
        r.name,
        r.description,
        r.color,
        r.iconB64 as `icon`
    FROM Route r
    INNER JOIN Transport t ON t.idTransport = r.idTransport
    WHERE r.idTransport = pIdTransport
    ORDER BY name;

END$$

-- -----------------------------------------------------------------------------
  -- getStopsFromRoute
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getStopsFromRoute$$
CREATE PROCEDURE getStopsFromRoute(
    `pIdRoute` int
)
BEGIN

    DECLARE `nextStopId` int;

    DROP TABLE IF EXISTS temp_StopsInRoute;
    CREATE TEMPORARY TABLE temp_StopsInRoute(
        `id` int,
        `name` varchar(200),
        `latitude` double,
        `longitude` double,
        `waypoints` varchar(1000),
        `icon` blob
    );

    -- Get terminal
    SET nextStopId = (SELECT idTerminal FROM Route WHERE idRoute = pIdRoute);

    -- Fill route with linked stops
    WHILE
        (SELECT COUNT(*) FROM temp_StopsInRoute WHERE id = nextStopId) = 0
    DO
        INSERT INTO temp_StopsInRoute(
            `id`,
            `name`,
            `latitude`,
            `longitude`,
            `icon`
        )
        SELECT
            s.idStop,
            s.name,
            s.lat,
            s.lon,
            s.iconB64
        FROM Stop s
        WHERE idStop = nextStopId;

        UPDATE temp_StopsInRoute
        SET waypoints = (
            SELECT
                GROUP_CONCAT(CONCAT(w.lon, ',', w.lat) SEPARATOR '|') as waypoints
            FROM Waypoint w 
            WHERE idStop = nextStopId
        )
        WHERE id = nextStopId;

        SET nextStopId = (SELECT idNext FROM Stop WHERE idStop = nextStopId);
    END WHILE;

    SELECT * FROM temp_StopsInRoute;
    DROP TABLE temp_StopsInRoute;

END$$

-- -----------------------------------------------------------------------------
  -- getDriverInfo
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getDriverInfo$$
CREATE PROCEDURE getDriverInfo(
    `pToken` varchar(40)
)
sp: BEGIN

    IF (SELECT COUNT(*) FROM Vehicle v WHERE v.driverToken = pToken AND v.disabled = 1) > 0 THEN
        SELECT 1 AS error, 'disabled-vehicle' as message;
        LEAVE sp;
    END IF;

    SELECT DISTINCT
        0 as error,
        d.name,
        t.name as 'transportName',
        v.identifier AS 'vehicleIdentifier',
        CONCAT(r.name, ' - ', r.description) AS 'routeName',
        r.color AS 'routeColor',
        r.iconB64 AS 'routeIcon',
        r.idRoute AS 'idRoute'
    FROM Driver d
        INNER JOIN Driver_Vehicle dv ON d.idDriver = d.idDriver
        INNER JOIN Vehicle v ON dv.idVehicle = v.idVehicle
        INNER JOIN Vehicle_Route vr ON v.idVehicle = vr.idVehicle
        INNER JOIN Route r ON vr.idRoute = r.idRoute
        INNER JOIN Transport t ON r.idTransport = t.idTransport
    WHERE d.token = pToken
        AND v.driverToken IS NULL
        AND v.disabled = 0;

END$$

-- -----------------------------------------------------------------------------
  -- useVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS useVehicle$$
CREATE PROCEDURE useVehicle(
    `pVehicleIdentifier` varchar(255),
    `pToken` varchar(40)
)
sp: BEGIN

    IF (SELECT disabled FROM Vehicle WHERE identifier = pVehicleIdentifier) THEN
        SELECT 2 as error, 'vehicle-disabled' as message;
        LEAVE sp;
    END IF;
    IF (SELECT COUNT(*) FROM Vehicle WHERE identifier = pVehicleIdentifier AND driverToken <> pToken) > 0 THEN
        SELECT 2 as error, 'vehicle-in-use' as message;
        LEAVE sp;
    END IF;

    UPDATE Vehicle
        SET driverToken = pToken
        WHERE identifier = pVehicleIdentifier;
    
    SELECT 0 as error;

END$$


-- -----------------------------------------------------------------------------
  -- leaveVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS leaveVehicle$$
CREATE PROCEDURE leaveVehicle(
    `pToken` varchar(40)
)
sp: BEGIN

    IF (SELECT COUNT(*) FROM Vehicle WHERE driverToken = pToken) = 0 THEN
        SELECT 2 as error, 'vehicle-not-in-use' as message;
        LEAVE sp;
    END IF;

    UPDATE Vehicle
        SET driverToken = NULL
        WHERE driverToken = pToken;
    
    SELECT 0 as error;

END$$


-- -----------------------------------------------------------------------------
  -- endTrip
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS endTrip$$
CREATE PROCEDURE endTrip(
    `pToken` varchar(40)
)
sp: BEGIN

    DECLARE vIdVehicle INT;


    SET vIdVehicle = (SELECT v.idVehicle FROM Vehicle v
        WHERE driverToken = pToken);

    IF vIdVehicle IS NULL THEN
        SELECT 2 as error, 'invalid-token' as message;
        LEAVE sp;
    END IF;
    IF (SELECT disabled FROM Vehicle WHERE idVehicle = vIdVehicle) = 1 THEN
        SELECT 1 as error, 'disabled-vehicle' as message;
        LEAVE sp;
    END IF;


    UPDATE Vehicle
        SET driverToken = NULL
        WHERE driverToken = pToken;
    DELETE FROM Last_Location
        WHERE idVehicle = vIdVehicle;
    DELETE FROM VehicleData
        WHERE idVehicle = vIdVehicle;
    
    SELECT 0 as error;

END$$


-- -----------------------------------------------------------------------------
  -- startTrip
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS startTrip$$
CREATE PROCEDURE startTrip(
    `pToken` varchar(40),
    `pIdRoute` int
)
sp: BEGIN

    DECLARE vIdVehicle INT;
    DECLARE vIdStop INT;

    IF (SELECT COUNT(*) FROM Vehicle WHERE driverToken = pToken AND disabled = 0) = 0 THEN
        SELECT 2 as error, 'invalid-token' as message;
        LEAVE sp;
    END IF;


    SET vIdVehicle = (SELECT v.idVehicle FROM Vehicle v
        WHERE driverToken = pToken);
    SET vIdStop = (SELECT idTerminal FROM Route
        WHERE idRoute = pIdRoute);
    
    DELETE FROM Last_Location
        WHERE idVehicle = vIdVehicle;
    INSERT INTO Last_Location(idVehicle, idLastStop, lat, lon)
        SELECT vIdVehicle, s.idStop, s.lat, s.lon
            FROM Stop s
            WHERE idStop = vIdStop;

    DELETE FROM VehicleData
        WHERE idVehicle = vIdVehicle;
    INSERT INTO VehicleData(idVehicle, direction, distanceToStop, avgSpeed)
        SELECT vIdVehicle, 0, s.distanceTo, 1
            FROM Stop s
            WHERE s.idStop = (
                SELECT idNext FROM Stop WHERE idStop = vIdStop
        );
    
    SELECT
        0 as error,
        vIdVehicle,
        vIdStop,
        (SELECT s.distanceTo FROM Stop s WHERE s.idStop = (
                SELECT idNext FROM Stop WHERE idStop = vIdStop
            )
        ) as distanceTo;

END$$


-- -----------------------------------------------------------------------------
  -- cancelTrip
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS cancelTrip$$
CREATE PROCEDURE cancelTrip(
    `pToken` varchar(40),
    `pReason` varchar(200)
)
sp: BEGIN

    IF (SELECT COUNT(*) FROM Vehicle WHERE driverToken = pToken) = 0 THEN
        SELECT 2 as error, 'invalid-token' as message;
        LEAVE sp;
    END IF;

    INSERT INTO CancelReason(time, reason, idDriver)
        VALUES(NOW(), pReason, (SELECT idDriver FROM Driver WHERE token = pToken));
    
    UPDATE Vehicle
        SET disabled = 1
        WHERE driverToken = pToken;

    SELECT 0 as error;

END$$


-- -----------------------------------------------------------------------------
  -- getIncidents
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getIncidents$$
CREATE PROCEDURE getIncidents(
    `pIdRoute` int
)
BEGIN

    SELECT
        i.idIncident AS id,
        it.name AS name,
        it.idIncidentType AS type,
        i.description AS description,
        i.lon AS lon,
        i.lat AS lat
    FROM Incident i
        INNER JOIN IncidentType it ON it.idIncidentType = i.idIncidentType
    WHERE i.idRoute = 60001;

END$$

-- -----------------------------------------------------------------------------
  -- addIncident
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS addIncident$$
CREATE PROCEDURE addIncident(
    `pIncidentType` int,
    `pIdRoute` int,
    `pLon` double,
    `pLat` double,
    `pToken` varchar(40),
    `pDescription` varchar(255)
)
sp: BEGIN

    DECLARE vNewId INT;

    IF (SELECT COUNT(*) FROM IncidentType it WHERE it.idIncidentType = pIncidentType) = 0 THEN
        SELECT 1 AS error, 'invalid-type' AS message;
        LEAVE sp;
    END IF;
    IF (SELECT COUNT(*) FROM Driver d WHERE d.token = pToken) = 0 THEN
        SELECT 2 AS error, 'invalid-token' AS message;
        LEAVE sp;
    END IF;

    INSERT INTO Incident (idRoute, description, lon, lat, idIncidentType)
    VALUES (pIdRoute, pDescription, pLon, pLat, pIncidentType);

    SET vNewId = LAST_INSERT_ID();

    INSERT INTO Driver_Incident(idIncident, idDriver)
    VALUES (
        vNewId, (SELECT idDriver FROM Driver d WHERE d.token = pToken)
    );

    SELECT 0 AS error, vNewId as `idIncident`;

END$$

-- -----------------------------------------------------------------------------
  -- removeIncident
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS removeIncident$$
CREATE PROCEDURE removeIncident(
    `pIdIncident` int,
    `pToken` varchar(40)
)
sp: BEGIN

    IF (SELECT COUNT(*) FROM Incident i WHERE i.idIncident = pIdIncident) = 0 THEN
        SELECT 1 AS error, 'incident-not-found' AS message;
        LEAVE sp;
    END IF;
    IF (SELECT COUNT(*) FROM Driver d WHERE d.token = pToken) = 0 THEN
        SELECT 2 AS error, 'invalid-token' AS message;
        LEAVE sp;
    END IF;

    DELETE FROM Driver_Incident
        WHERE idIncident = pIdIncident;
    DELETE FROM Incident
        WHERE idIncident = pIdIncident;

    SELECT 0 AS error;

END$$

-- -----------------------------------------------------------------------------
  -- setDriverActive
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS setDriverActive$$
CREATE PROCEDURE setDriverActive(
    `pToken` varchar(40),
    `pIdentifier` varchar(40)
)
sp: BEGIN

    DECLARE error int;

    IF (SELECT COUNT(*) FROM Driver d WHERE d.token = pToken) = 0 THEN
        SELECT 1 AS error;
        LEAVE sp;
    END IF;

    IF (SELECT active FROM Driver d WHERE d.token = pToken) = 0 THEN
        SELECT 2 AS error;
        LEAVE sp;
    END IF;
    
    UPDATE Vehicle SET
        token = pToken WHERE identifier = pIdentifier;
    UPDATE Driver SET
        active = 1 WHERE token = pToken;

    SELECT 0 AS error;

END$$


-- -----------------------------------------------------------------------------
  -- reportLocation
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS reportLocation$$
CREATE PROCEDURE reportLocation(
    `pLat` double,
    `pLon` double,
    `pToken` varchar(40),
    `pSpeed` float
)
sp: BEGIN

    DECLARE vIdVehicle int;
    DECLARE haversineDelta float;
    DECLARE defaultIdStop int;
    DECLARE vBearing double;
    DECLARE distanceToNext float;
    DECLARE distanceToLast float;
    DECLARE vIdStop int;
    DECLARE vIdNextStop int;
    DECLARE vSpeed float;
    
    SET vIdVehicle = (SELECT idVehicle FROM Vehicle v WHERE v.driverToken = pToken);
    SET defaultIdStop = ( -- First stop in route
        SELECT idTerminal FROM Route r
        INNER JOIN Vehicle_Route vr ON r.idRoute = vr.idRoute
        WHERE vr.idVehicle = vIdVehicle
    );
    IF vIdVehicle IS NULL THEN
        SELECT 2 AS error, 'invalid-token' AS message;
        LEAVE sp;
    END IF;

    -- New value, generate new location
    IF (SELECT COUNT(*) FROM Last_Location ll WHERE ll.idVehicle = vIdVehicle) = 0 THEN
        INSERT INTO Last_Location(
            idVehicle, idLastStop, lon, lat, time
        ) VALUES (
            vIdVehicle, defaultIdStop, pLon, pLat, NOW()
        );

        INSERT INTO VehicleData (
            idVehicle, direction, distanceToStop, avgSpeed
        ) VALUES (
            vIdVehicle,
            0,
            (SELECT distanceTo FROM Stop WHERE idStop = (SELECT idNext FROM Stop WHERE idStop = defaultIdStop)),
            0
        );
    -- Existing value, update it
    ELSE
        
        CALL getHaversineDelta(
            (SELECT lon FROM Last_Location WHERE idVehicle = vIdVehicle),
            (SELECT lat FROM Last_Location WHERE idVehicle = vIdVehicle),
            pLon,
            pLat,
            haversineDelta
        );
        SET vBearing = (
            SELECT
                DEGREES(ATAN2(pLon-lon, pLat-lat))
            FROM Last_Location
            WHERE idVehicle = vIdVehicle
        );
        SET vBearing = MOD(vBearing+360, 360);
        SET vIdStop = (
            SELECT s.idStop FROM Stop s
                INNER JOIN Stop sl ON sl.idNext = s.idStop
                INNER JOIN Last_Location ll ON sl.idStop = ll.idLastStop
                WHERE ll.idVehicle = vIdVehicle
        );
        SET vIdNextStop = (
            SELECT idStop FROM Stop WHERE idStop = (SELECT idNext FROM Stop WHERE idStop = vIdStop)
        );
        SET vSpeed = TIMESTAMPDIFF(
            SECOND,
            (SELECT ll.time FROM Last_Location ll WHERE idVehicle = vIdVehicle),
            NOW()
        );
        SET vSpeed = (
            haversineDelta / IF(vSpeed = 0, 1, vSpeed)
        );
        CALL getHaversineDelta(
            (SELECT lon FROM Stop WHERE idStop = vIdStop),
            (SELECT lat FROM Stop WHERE idStop = vIdStop),
            pLon,
            pLat,
            distanceToNext
        );


        INSERT INTO testValues(speed, haversineDelta, tDiff)
            VALUES(
                vSpeed,
                haversineDelta,
                TIMESTAMPDIFF(
                    SECOND,
                    (SELECT ll.time FROM Last_Location ll WHERE idVehicle = vIdVehicle),
                    NOW()
                )
            );


        UPDATE VehicleData SET
            direction = vBearing
            WHERE idVehicle = vIdVehicle;
        UPDATE Last_Location SET
            time = NOW()
            WHERE idVehicle = vIdVehicle;
        
        IF (distanceToNext < 20) THEN
            IF (SELECT inStop FROM VehicleData WHERE idVehicle = vIdVehicle) = 0 THEN
                UPDATE VehicleData SET
                    inStop = 1;
            END IF;

            UPDATE Last_Location SET
                lon = (SELECT lon FROM Stop WHERE idStop = vIdStop),
                lat = (SELECT lat FROM Stop WHERE idStop = vIdStop)
                WHERE idVehicle = vIdVehicle;

            UPDATE VehicleData SET
                distanceToStop = 0
                WHERE idVehicle = vIdVehicle;
        ELSE
            IF (SELECT inStop FROM VehicleData WHERE idVehicle = vIdVehicle) = 1 THEN
                UPDATE VehicleData SET
                    inStop = 0;
                
                UPDATE Last_Location SET
                    idLastStop = vIdStop
                    WHERE idVehicle = vIdVehicle;

                UPDATE VehicleData SET
                    distanceToStop = (SELECT distanceTo FROM Stop WHERE idStop = vIdNextStop)
                    WHERE idVehicle = vIdVehicle;
            ELSE
                UPDATE VehicleData SET
                    distanceToStop = GREATEST(0, distanceToStop - haversineDelta)
                    WHERE idVehicle = vIdVehicle;
            END IF;

            UPDATE Last_Location SET
                lon = pLon,
                lat = pLat
                WHERE idVehicle = vIdVehicle;
        END IF;
        
    END IF;
    
    -- TODO Documentar
    -- alpha for the EWMA (Exponentially Weighted Moving Average)
    SET @alpha = 0.1;
    UPDATE VehicleData SET
        avgSpeed = GREATEST(@alpha * vSpeed + (1 - @alpha) * avgSpeed, 0.1)
        WHERE idVehicle = vIdVehicle;

    SELECT 0 AS error, avgSpeed as avgSpeed, distanceToNext
        FROM VehicleData
        WHERE idVehicle = vIdVehicle;

END$$


-- -----------------------------------------------------------------------------
  -- getLocations
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getLocations$$
CREATE PROCEDURE getLocations(
    `pIdRoute` int
)
BEGIN

    SELECT
        v.identifier,
        ll.lon as lon,
        ll.lat as lat,
        (vd.direction + 90) as direction
    FROM Vehicle v
        INNER JOIN Last_Location ll ON v.idVehicle = ll.idVehicle
        INNER JOIN Vehicle_Route vr ON v.idVehicle = vr.idVehicle
        INNER JOIN VehicleData vd ON v.idVehicle = vd.idVehicle
        WHERE vr.idRoute = pIdRoute
        AND v.driverToken IS NOT NULL
        AND v.disabled = 0;

END$$

-- -----------------------------------------------------------------------------
  -- arrivedToStop
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS arrivedToStop$$
CREATE PROCEDURE arrivedToStop(
    `pToken` varchar(40),
    `pIdStop` int
)
sp: BEGIN

    DECLARE vIdVehicle INT;

    SET vIdVehicle = (SELECT idVehicle FROM Vehicle WHERE driverToken = pToken);
    IF vIdVehicle IS NULL THEN
        SELECT 2 AS error, 'invalid-token' AS message;
        LEAVE sp;
    END IF;

    UPDATE VehicleData SET
        inStop = 1
        WHERE idVehicle = vIdVehicle;
    UPDATE Last_Location SET
        idLastStop = (SELECT idStop FROM Stop WHERE idNext = pIdStop)
        WHERE idVehicle = vIdVehicle;
    
    SELECT 0 AS error;

END$$

-- -----------------------------------------------------------------------------
  -- getWaitTimeForStop
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getWaitTimeForStop$$
CREATE PROCEDURE getWaitTimeForStop(
    `pIdRoute` int,
    `pIdStop` int
)
sp: BEGIN

    DECLARE vAvgSpeed float;
    DECLARE vCurrStop int;
    DECLARE vTotalDistance float;
    DECLARE vDistanceToNext float;
    DECLARE vTimePadding float;
    DECLARE vIdVehicle int;
    DECLARE vIdNextStop int;

    IF (SELECT COUNT(*) FROM Stop WHERE idStop = pIdStop) = 0 THEN
        SELECT 1 AS error, 'invalid-stop-id' AS message;
        LEAVE sp;
    END IF;
    IF (SELECT COUNT(*) FROM Route WHERE idRoute = pIdRoute) = 0 THEN
        SELECT 1 AS error, 'invalid-route-id' AS message;
        LEAVE sp;
    END IF;
    IF (
        SELECT COUNT(*) FROM Last_Location ll
        INNER JOIN Vehicle v ON ll.idVehicle = v.idVehicle
        WHERE v.driverToken IS NOT NULL
            AND v.disabled = 0
    ) = 0 THEN
        SELECT 3 AS error, 'no-vehicles' AS message;
        LEAVE sp;
    END IF;

    SET vCurrStop = pIdStop;
    SET vTotalDistance = 0;
    SET vDistanceToNext = 0;
    SET vTimePadding = 0;

    infWhile: WHILE 1=1 DO
        SET vTotalDistance = vTotalDistance + (SELECT distanceTo FROM Stop WHERE idStop = vCurrStop);
        SET vTimePadding = vTimePadding + 5;

        SET vCurrStop = (SELECT idStop FROM Stop WHERE idNext = vCurrStop);

        IF (
            SELECT COUNT(*) FROM Last_Location ll
            INNER JOIN Vehicle v ON v.idVehicle = ll.idVehicle
            WHERE ll.idLastStop = vCurrStop
                AND v.disabled = 0
        ) > 0 THEN
            LEAVE infWhile;
        END IF;
    END WHILE;
    
    SET vIdVehicle = (SELECT idVehicle FROM Last_Location WHERE idLastStop = vCurrStop LIMIT 1);
    IF (SELECT COUNT(*) FROM Last_Location WHERE idVehicle = vIdVehicle AND idLastStop = pIdStop) = 0 THEN
        SET vDistanceToNext = (SELECT GREATEST(distanceToStop, 0) FROM VehicleData WHERE idVehicle = vIdVehicle);
    END IF;
    SET vTotalDistance = vTotalDistance + vDistanceToNext;
    SET vIdNextStop = (SELECT idNext FROM Stop WHERE idStop = vCurrStop);
    SET vAvgSpeed = (SELECT avgSpeed FROM VehicleData WHERE idVehicle = vIdVehicle);

    SELECT
        0 AS error,
        identifier as identifier,
        (SELECT name FROM Stop WHERE idStop = vIdNextStop) as nextName,
        vDistanceToNext as nextDistance,
        (vDistanceToNext / vAvgSpeed) + vTimePadding as nextTime,
        vTotalDistance as totalDistance,
        (vTotalDistance / vAvgSpeed) + vTimePadding AS totalTime,
        (
            SELECT
                (
                    (s.idNext = pIdStop AND vd.inStop)
                    OR
                    (ll.idLastStop = pIdStop)
                )
            FROM Last_Location ll 
                INNER JOIN VehicleData vd ON vd.idVehicle = ll.idVehicle
                INNER JOIN Stop s ON s.idStop = ll.idLastStop 
                INNER JOIN Route r ON r.idRoute = s.idRoute
                WHERE ll.idVehicle = vIdVehicle
        ) as arrived
    FROM Vehicle v
        INNER JOIN VehicleData vd on vd.idVehicle = v.idVehicle
        WHERE v.idVehicle = vIdVehicle;

END$$

-- -----------------------------------------------------------------------------
  -- getWaitTimeForVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getWaitTimeForVehicle$$
CREATE PROCEDURE getWaitTimeForVehicle(
    `pIdRoute` int,
    `pIdStop` int,
    `pIdentifier` varchar(40)
)
sp: BEGIN

    DECLARE vAvgSpeed float;
    DECLARE vCurrStop int;
    DECLARE vTotalDistance float;
    DECLARE vDistanceToNext float;
    DECLARE vTimePadding float;
    DECLARE vIdVehicle int;
    DECLARE vIdNextStop int;

    SET vIdVehicle = (SELECT idVehicle FROM Vehicle WHERE identifier = pIdentifier);

    IF vIdVehicle IS NULL THEN
        SELECT 1 AS error, 'invalid-vehicle-id' AS message;
    END IF;
    IF (SELECT COUNT(*) FROM Stop WHERE idStop = pIdStop) = 0 THEN
        SELECT 1 AS error, 'invalid-stop-id' AS message;
        LEAVE sp;
    END IF;
    IF (SELECT COUNT(*) FROM Route WHERE idRoute = pIdRoute) = 0 THEN
        SELECT 1 AS error, 'invalid-route-id' AS message;
        LEAVE sp;
    END IF;
    IF (
        SELECT COUNT(*) FROM Last_Location ll
            INNER JOIN Vehicle v ON ll.idVehicle = v.idVehicle
            WHERE v.driverToken IS NOT NULL
            AND v.disabled = 0
    ) = 0 THEN
        SELECT 3 AS error, 'no-vehicles' AS message;
        LEAVE sp;
    END IF;

    -- establecer estación como la estación a la que queremos llegar
    SET vCurrStop = pIdStop;
    SET vTotalDistance = 0;
    SET vDistanceToNext = 0; 

    -- mientras no haya vehiculos que acaben de haber pasado por la estación
    WHILE (SELECT COUNT(*) FROM Last_Location WHERE idLastStop = vCurrStop AND idVehicle = vIdVehicle) = 0 DO
        SET vTotalDistance = vTotalDistance + (SELECT distanceTo FROM Stop WHERE idStop = vCurrStop);
        SET vTimePadding = vTimePadding + 5;

        SET vCurrStop = (SELECT idStop FROM Stop WHERE idNext = vCurrStop);
    END WHILE;
    
    
    SET vDistanceToNext = (SELECT GREATEST(distanceToStop, 0) FROM VehicleData WHERE idVehicle = vIdVehicle);
    SET vTotalDistance = vTotalDistance + vDistanceToNext;
    SET vIdNextStop = (SELECT idNext FROM Stop WHERE idStop = vCurrStop);
    SET vAvgSpeed = (SELECT avgSpeed FROM VehicleData WHERE idVehicle = vIdVehicle);

    SELECT
        0 AS error,
        identifier as identifier,
        (SELECT name FROM Stop WHERE idStop = vIdNextStop) as nextName,
        vDistanceToNext as nextDistance,
        (vDistanceToNext / vAvgSpeed) as nextTime,
        vTotalDistance as totalDistance,
        (vTotalDistance / vAvgSpeed) AS totalTime,
        vCurrStop,
        (
            SELECT
                (
                    (s.idNext = pIdStop AND vd.inStop)
                    OR
                    (ll.idLastStop = pIdStop)
                )
            FROM Last_Location ll 
                INNER JOIN VehicleData vd ON vd.idVehicle = ll.idVehicle
                INNER JOIN Stop s ON s.idStop = ll.idLastStop 
                INNER JOIN Route r ON r.idRoute = s.idRoute
                WHERE ll.idVehicle = vIdVehicle
        ) as arrived
    FROM Vehicle v
        INNER JOIN VehicleData vd on vd.idVehicle = v.idVehicle
        WHERE v.idVehicle = vIdVehicle;

END$$


-- -----------------------------------------------------------------------------
  -- getWaitTimeWithToken
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getWaitTimeWithToken$$
CREATE PROCEDURE getWaitTimeWithToken(
    `pToken` varchar(40)
)
sp: BEGIN

    DECLARE vAvgSpeed float;
    DECLARE vCurrStop int;
    DECLARE vTotalDistance float;
    DECLARE vDistanceToNext float;
    DECLARE vIdVehicle int;
    DECLARE vIdNextStop int;
    DECLARE vIdRoute int;

    SET vIdVehicle = (SELECT idVehicle FROM Vehicle WHERE driverToken = pToken);

    IF vIdVehicle IS NULL THEN
        SELECT 2 AS error, 'invalid-token' AS message;
    END IF;

    SET vIdRoute = (
        SELECT s.idRoute FROM Last_Location ll 
            INNER JOIN Stop s ON ll.idLastStop = s.idStop
            WHERE ll.idVehicle = vIdVehicle
    );
    SET vCurrStop = (
        SELECT ll.idLastStop FROM Last_Location ll
            WHERE ll.idVehicle = vIdVehicle
    );
    SET vTotalDistance = 0;
    SET vDistanceToNext = (SELECT GREATEST(distanceToStop, 0) FROM VehicleData WHERE idVehicle = vIdVehicle);
    SET vTotalDistance = vTotalDistance + vDistanceToNext;
    SET vIdNextStop = (SELECT idNext FROM Stop WHERE idStop = vCurrStop);
    SET vAvgSpeed = (SELECT avgSpeed FROM VehicleData WHERE idVehicle = vIdVehicle);

    SELECT
        0 AS error,
        v.identifier AS identifier,
        (SELECT name FROM Stop WHERE idStop = vIdNextStop) AS nextName,
        vDistanceToNext AS nextDistance,
        (vDistanceToNext / vAvgSpeed) AS nextTime,
        vTotalDistance AS totalDistance,
        (vTotalDistance / vAvgSpeed) AS totalTime,
        vIdVehicle,
        (
            SELECT
                (s.idNext = r.idTerminal AND vd.inStop)
            FROM Last_Location ll 
                INNER JOIN VehicleData vd ON vd.idVehicle = ll.idVehicle
                INNER JOIN Stop s ON s.idStop = ll.idLastStop 
                INNER JOIN Route r ON r.idRoute = s.idRoute
                WHERE ll.idVehicle = vIdVehicle
        ) as inTerminal
    FROM Vehicle v
        WHERE v.idVehicle = vIdVehicle;

END$$

-- -----------------------------------------------------------------------------
  -- setDriverInactive
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS setDriverInactive$$
CREATE PROCEDURE setDriverInactive(
    `pToken` varchar(40)
)
sp: BEGIN

    DECLARE vIdVehicle int;

    SET vIdVehicle = (SELECT idVehicle FROM Vehicle WHERE driverToken = pToken);

    IF vIdVehicle IS NULL THEN
        SELECT 1 AS error;
        LEAVE sp;
    END IF;
    
    DELETE FROM Last_Location
        WHERE idVehicle = vIdVehicle;
    DELETE FROM VehicleData
        WHERE idVehicle = vIdVehicle;

    UPDATE Vehicle SET
        driverToken = NULL WHERE driverToken = pToken;
    UPDATE Driver SET
        active = 0 WHERE token = pToken;
    SELECT 0 AS error;

END$$


-- -----------------------------------------------------------------------------
  -- findDriverSalt
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS findDriverSalt$$
CREATE PROCEDURE findDriverSalt(
    `pUsername` varchar(50)
)
BEGIN

    IF (SELECT COUNT(*) FROM Driver d WHERE d.username = pUsername) > 0 THEN
        SELECT d.salt as `salt` FROM Driver d WHERE d.username = pUsername;
    ELSE 
        SELECT 'not-found' as `salt`;
    END IF;

END$$


-- -----------------------------------------------------------------------------
  -- generateSalt
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS generateSalt$$
CREATE PROCEDURE generateSalt()
BEGIN

    DECLARE `newSalt` varchar(35);
    
    SET newSalt = MD5(RAND());
    WHILE (
        (SELECT COUNT(*) FROM Driver d WHERE d.salt = newSalt) +
        (SELECT COUNT(*) FROM Admin a WHERE a.salt = newSalt)
    ) > 0 DO
        SET newSalt = MD5(RAND());
    END WHILE;
    SELECT newSalt;

END$$


-- -----------------------------------------------------------------------------
  -- generateToken
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS generateToken$$
CREATE PROCEDURE generateToken(
    OUT tokenOut varchar(35)
)
BEGIN

    DECLARE `newToken` varchar(35);
    
    SET newToken = MD5(RAND());
    WHILE (
        (SELECT COUNT(*) FROM Driver d WHERE d.token = newToken)
    ) > 0 DO
        SET newToken = MD5(RAND());
    END WHILE;

    SET tokenOut = newToken;

END$$

-- -----------------------------------------------------------------------------
  -- generateOutToken
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS generateOutToken$$
CREATE PROCEDURE generateOutToken()
BEGIN

    DECLARE `newToken` varchar(40);
    CALL generateToken(newToken);
    SELECT newToken;

END$$


-- -----------------------------------------------------------------------------
  -- unlinkDriver
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS unlinkDriver$$
CREATE PROCEDURE unlinkDriver(
    pCurp varchar(50)
)
BEGIN

    DELETE FROM Driver_Vehicle
        WHERE idDriver = (
            SELECT idDriver FROM Driver WHERE curp = pCurp
        );

END$$


-- -----------------------------------------------------------------------------
  -- linkDriverVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS linkDriverVehicle$$
CREATE PROCEDURE linkDriverVehicle(
    pCurp varchar(50),
    pIdentifier varchar(255)
)
BEGIN

    DECLARE vIdDriver INT;
    DECLARE vIdVehicle INT;

    SET vIdDriver = (SELECT idDriver FROM Driver WHERE curp = pCurp);
    SET vIdVehicle = (SELECT idVehicle FROM Vehicle WHERE identifier = pIdentifier);

    IF (
        SELECT COUNT(*) FROM Driver_Vehicle
            WHERE idDriver = vIdDriver
            AND idVehicle = vIdVehicle
    ) = 0 THEN
        INSERT INTO Driver_Vehicle(idDriver, idVehicle) VALUES
            (vIdDriver, vIdVehicle); 
    END IF;

END$$


-- -----------------------------------------------------------------------------
  -- forgotPassword
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS forgotPassword$$
CREATE PROCEDURE forgotPassword(
    `pUsername` varchar(255)
)
sp: BEGIN

    IF (SELECT COUNT(*) FROM Driver WHERE username = pUsername) = 0 THEN
        SELECT 1 AS error, 'username-not-found' AS message;
    END IF;

    UPDATE Driver d SET
        d.requiresReset = 1
    WHERE d.username = pUsername;

    SELECT 0 AS error;

END$$


-- -----------------------------------------------------------------------------
--                 UPSERTS
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
  -- upsertDriver
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertDriver$$
CREATE PROCEDURE upsertDriver(
    `pCurp` varchar(20),
    `pName` varchar(255),
    `pSurnameP` varchar(255),
    `pSurnameM` varchar(255),
    `pPassword` varchar(100),
    `pSalt` varchar(40),
    `pPhone` varchar(30),
    `pActive` tinyint
)
BEGIN

    DECLARE `newUsername` varchar(50);

    -- TODO newUsername generation logic
    SET newUsername = pCurp;

    IF (SELECT COUNT(*) FROM Driver d WHERE d.curp = pCurp) > 0 THEN
        UPDATE Driver d SET
            d.name = pName,
            d.surnameP = pSurnameP,
            d.surnameM = pSurnameM,
            d.username = newUsername,
            d.password = pPassword,
            d.salt = pSalt,
            d.phone = pPhone,
            d.active = pActive
        WHERE d.curp = pCurp;
        SELECT d.idDriver FROM Driver d WHERE d.curp = pCurp;
    ELSE 
        INSERT INTO Driver(
            curp, name, surnameP, surnameM, username, password, salt, phone, active
        ) VALUES (
            pCurp, pName, pSurnameP, pSurnameM, newUsername, pPassword, pSalt, pPhone, pActive
        );
        SELECT LAST_INSERT_ID();
    END IF;

END$$


-- -----------------------------------------------------------------------------
  -- upsertStop
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertStop$$
CREATE PROCEDURE upsertStop(
    `pIdRoute` int,
    `pName` varchar(255),
    `plon` double,
    `plat` double,
    `pNameNext` varchar(255)
)
BEGIN
    DECLARE `vCurrIdStop` int;
    DECLARE `vIdNext` int;

    IF (SELECT COUNT(*) FROM Stop s WHERE s.idRoute = pIdRoute AND s.name = pNameNext ) > 0 THEN
        SET vIdNext = (SELECT idStop FROM Stop s WHERE s.idRoute = pIdRoute AND s.name = pNameNext);
    ELSE
        SET vIdNext = NULL;
    END IF;

    IF (SELECT COUNT(*) FROM Stop s WHERE s.idRoute = pIdRoute AND s.name = pName) > 0 THEN
        SET vCurrIdStop = (SELECT s.idStop FROM Stop s WHERE s.idRoute = pIdRoute AND s.name = pName);
        
        -- remove from chain
        UPDATE Stop s SET
            s.idNext = (SELECT idNext FROM Stop WHERE idStop = vCurrIdStop)
        WHERE s.idNext = vCurrIdStop;
        
        UPDATE Stop s SET
            s.lon = plon,
            s.lat = plat,
            s.idNext = -1
        WHERE s.idStop = vCurrIdStop;
    ELSE
        INSERT INTO Stop(
            idRoute, name, lon, lat, idNext
        ) VALUES (
            pIdRoute, pName, plon, plat, -1
        );

        SET vCurrIdStop = LAST_INSERT_ID();
    END IF;

    -- link into chain
    UPDATE Stop s SET
        s.idNext = vCurrIdStop
    WHERE idNext = vIdNext
        AND idRoute = pIdRoute;

    UPDATE Stop s SET
        s.idNext = vIdNext
    WHERE idStop = vCurrIdStop
        AND idRoute = pIdRoute;

    SELECT vCurrIdStop;

END$$


-- -----------------------------------------------------------------------------
  -- upsertRoute
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertRoute$$
CREATE PROCEDURE upsertRoute(
    `pIdRoute` int,
    `pIdTransport` int,
    `pName` varchar(200),
    `pDescription` varchar(200)
)
BEGIN

    IF (SELECT COUNT(*) FROM Route WHERE idRoute = pIdRoute) > 0 THEN
        UPDATE Route r SET
            r.idTransport = pIdTransport,
            r.name = pName,
            r.description = pDescription
        WHERE r.idRoute = pIdRoute;
        SELECT pIdRoute;
    ELSE
        INSERT INTO Route(
            idTransport, name, description
        ) VALUES (
            pIdTransport, pName, pDescription
        );
        SELECT LAST_INSERT_ID();
    END IF;
END$$

-- -----------------------------------------------------------------------------
  -- upsertVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertVehicle$$
CREATE PROCEDURE upsertVehicle(
       `pIdentificador` varchar(255),
    `pIdconductor` int,
    `pCurp` varchar(255)
)
BEGIN

    IF (SELECT COUNT(*) FROM Vehicle WHERE identifier = pIdentificador) > 0 THEN
        UPDATE Vehicle r SET
            r.identifier = pIdentificador
        WHERE r.identifier = pIdentificador;
        SELECT pIdentificador;
    ELSE
        INSERT INTO Vehicle(
            identifier
        ) VALUES (
            pIdentificador
        );
       
           CALL nuevoStoredProcedure(pCurp, pIdentificador);
        SELECT LAST_INSERT_ID();
    END IF;
END$$

-- -----------------------------------------------------------------------------
  -- upsertTransport
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertTransport$$
CREATE PROCEDURE upsertTransport(
       `pName` varchar(255)
)
BEGIN

    IF (SELECT COUNT(*) FROM Transport WHERE name = pName) > 0 THEN
        UPDATE Transport r SET
            r.name = pName
        WHERE r.name = pName;
        SELECT pName;
    ELSE
        INSERT INTO Transport(
            name
        ) VALUES (
            pName
        );
        SELECT LAST_INSERT_ID();
    END IF;
END$$


-- -----------------------------------------------------------------------------
  -- loginAdmin
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS loginAdmin$$
CREATE PROCEDURE loginAdmin(
    `pUsername` varchar(100),
    `pPassword` varchar(255)
)
BEGIN

    DECLARE `login` int;
    DECLARE `newToken` varchar(40);
    CALL generateAdminToken(newToken);

    SET login = (SELECT count(*) AS auth FROM Admin a
        WHERE a.username = pUsername
        AND a.password = pPassword);

    UPDATE Admin d
        SET d.token = newToken
        WHERE d.username = pUsername;

    IF login = 1 THEN
        SELECT login, newToken as token;
    ELSE
        SELECT login;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- findAdminSalt
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS findAdminSalt$$
CREATE PROCEDURE findAdminSalt(
    `pUsername` varchar(50)
)
BEGIN

    IF (SELECT COUNT(*) FROM Admin a WHERE a.username = pUsername) > 0 THEN
        SELECT a.salt as `salt` FROM Admin a WHERE a.username = pUsername;
    ELSE 
        SELECT 'not-found' as `salt`;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- generateAdminToken
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS generateAdminToken$$
CREATE PROCEDURE generateAdminToken(
    OUT tokenOut varchar(35)
)
BEGIN

    DECLARE `newToken` varchar(35);
    
    SET newToken = MD5(RAND());
    WHILE (
        (SELECT COUNT(*) FROM Admin a WHERE a.token = newToken)
    ) > 0 DO
        SET newToken = MD5(RAND());
    END WHILE;

    SET tokenOut = newToken;

END$$

-- -----------------------------------------------------------------------------
  -- logoutAdmin
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS logoutAdmin$$
CREATE PROCEDURE logoutAdmin(
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Admin a WHERE a.token = pToken) > 0 THEN
        UPDATE Driver d SET
            d.token = NULL WHERE d.token = pToken;

        SELECT 1 AS logout;
    ELSE
        SELECT 0 AS logout;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- getRoutesAdmin
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getRoutesAdmin$$
CREATE PROCEDURE getRoutesAdmin()
BEGIN

    SELECT idRoute, idTransport , name, description , color 
    FROM Chuchu.Route;

END$$

-- -----------------------------------------------------------------------------
  -- getTransportsAdmin
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getTransportsAdmin$$
CREATE PROCEDURE getTransportsAdmin()
BEGIN

    SELECT
        t.idTransport AS `id`,
        t.name as `name`,
        FROM_BASE64(t.iconB64) as `icon`
    FROm Chuchu.Transport t
    ORDER BY name;

END$$

-- -----------------------------------------------------------------------------
  -- getVehiclesAdmin 
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getVehiclesAdmin$$
CREATE PROCEDURE getVehiclesAdmin()
BEGIN

    SELECT *
    FROM (
        SELECT 
            D.idVehicle, 
            D.identifier, 
            D.idRoute, 
            R.description, 
            D.idDriver, 
            DR.username
        FROM (
            SELECT 
                V.idVehicle, 
                V.identifier, 
                VR.idRoute, 
                DV.idDriver
            FROM Chuchu.Vehicle V
            LEFT JOIN Chuchu.Vehicle_Route VR ON V.idVehicle = VR.idVehicle
            LEFT JOIN Chuchu.Driver_Vehicle DV ON V.idVehicle = DV.idVehicle
        ) AS D
        LEFT JOIN Chuchu.Driver DR ON D.idDriver = DR.idDriver
        LEFT JOIN Chuchu.Route R ON D.idRoute = R.idRoute
    ) AS VECHDATA;

END$$

-- -----------------------------------------------------------------------------
  -- getDriversAdmin
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getDriversAdmin$$
CREATE PROCEDURE getDriversAdmin()
BEGIN

    SELECT idDriver, curp, name, surnameP, surnameM, username, phone, active, requiresReset
    FROM Chuchu.Driver;

END$$

-- -----------------------------------------------------------------------------
  -- getStopsAdmin
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getStopsAdmin$$
CREATE PROCEDURE getStopsAdmin()
BEGIN

    SELECT idStop, idRoute, name , lat , lon , distanceTo, idNext 
    FROM Chuchu.Stop;

END$$


-- -----------------------------------------------------------------------------
  -- freeVehicles
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS freeVehicles$$
CREATE PROCEDURE freeVehicles()
BEGIN

    DELETE FROM VehicleData;
    DELETE FROM Last_Location;
    UPDATE Vehicle SET
        driverToken = NULL,
        disabled = 0;
END$$


-- -----------------------------------------------------------------------------
  -- resetData
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS resetData$$
CREATE PROCEDURE resetData()
BEGIN

    CALL freeVehicles();
    UPDATE Driver SET
        token = NULL,
        requiresReset = 0;

END$$


-- -----------------------------------------------------------------------------
  -- testDriverData
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS testDriverData$$
CREATE PROCEDURE testDriverData(
    pToken varchar(40)
)
BEGIN

	CALL startTrip(pToken, 60001);
    CALL reportLocation(19.49536191604533, -99.1362979935579, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495326158585893, -99.13671524732746, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495569309154284, -99.1368973216996, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495841065239578, -99.13683663024223, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49634881880768, -99.13674559305615, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496570513527914, -99.13672283375962, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497021053475198, -99.13660145084484, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497435835174837, -99.1365331729553, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497736193672957, -99.1364724814979, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49789173625311, -99.13644403237728, pToken, 10);   -- esime 1
    CALL arrivedToStop(pToken, 45);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49802034202767, -99.13643522327206, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498250425571573, -99.13634402817226, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.4983566178661, -99.13633061712818, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49849315071383, -99.13631452387528, pToken, 10);   -- esime 2
    CALL arrivedToStop(pToken, 44);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49867197332582, -99.13627502252169, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498842061839227, -99.136241190625, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499016402379908, -99.13620284780875, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49913546410463, -99.13619382596963, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499277912838746, -99.13618254867072, pToken, 10);  -- esime
    CALL arrivedToStop(pToken, 43);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49951748578027, -99.1361129809769, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49964376941575, -99.13609543761694, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499797113697788, -99.13606513544975, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499945947714885, -99.13603642813347, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.5001173321831, -99.13600134140536, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50029446371262, -99.13596607273058, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500462841062802, -99.13595490877425, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50063723170506, -99.1359054683962, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500838682741456, -99.13586400227014, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500957448526822, -99.135849648612, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501052160419768, -99.1358193464448, pToken, 10);   -- edif 6 esiqie
    CALL arrivedToStop(pToken, 42);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501166033522917, -99.13581284362465, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501378759089324, -99.13576180839571, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501528343330733, -99.13573948048302, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50171400800896, -99.13569801435418, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501927484532274, -99.13566213020881, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502105631984318, -99.13564219456775, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502230410252228, -99.13559993101731, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502341658261315, -99.1355664391483, pToken, 10);   -- edif 8 esiqie
    CALL arrivedToStop(pToken, 41);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502553630614532, -99.13554092152434, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50267465016275, -99.1355098219317, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50282423320596, -99.13548350689177, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503008393051783, -99.13545400214619, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503163989159756, -99.13542688968082, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503339880236897, -99.13539180295825, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503436094079632, -99.1352514560765, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503531556192492, -99.13519563628145, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503737513627623, -99.13521716676865, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50380366049215, -99.1354300793847, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50384349892757, -99.13562943574782, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503879579011574, -99.13576739035108, pToken, 10);  -- edif 10 esia
    CALL arrivedToStop(pToken, 40);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50393445079409, -99.13612623184473, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503989322555285, -99.13642207670122, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504026905942055, -99.13664057127518, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504058475980198, -99.13686385040185, pToken, 10);  -- edif 11 esia
    CALL arrivedToStop(pToken, 39);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504125666578396, -99.13723701969339, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50414210050792, -99.13739661111802, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504204043765746, -99.13769567740117, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50427483603138, -99.13799340257991, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504321609475607, -99.13821736701617, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504345628271366, -99.1383984161471, pToken, 10);   -- biblioteca
    CALL arrivedToStop(pToken, 38);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504418241775596, -99.1388202643998, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504420770067977, -99.13914078835347, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504506731985604, -99.13947606445565, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504555360536397, -99.13972119968751, pToken, 10);  -- secretaria
    CALL arrivedToStop(pToken, 37);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50458064343792, -99.14003702979235, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504656492116165, -99.14040851571356, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50472981247472, -99.14087924342525, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504779114074168, -99.14113539436731, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50484927401819, -99.14151358584456, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50489351901232, -99.14178851226855, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504940561947436, -99.14214394570263, pToken, 10);  -- central de inteligencia
    CALL arrivedToStop(pToken, 36);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505008193538103, -99.14249129176446, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505065079901115, -99.14278767585819, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50508973065163, -99.14297677157981, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50511374932957, -99.14317994892345, pToken, 10);   -- Ma. Luisa Stampa
    CALL arrivedToStop(pToken, 35);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505174428075648, -99.14357959806682, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505207295720417, -99.14385988890977, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505254613182135, -99.14414997727587, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505340574659392, -99.14461601109576, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505430328505106, -99.1448781970296, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505537866088595, -99.14510822952644, pToken, 10);  -- fte escom
    CALL arrivedToStop(pToken, 34);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50574479461253, -99.14540372927168, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505852060059578, -99.1455971832921, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505989717279075, -99.1458209830412, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50617028046728, -99.1460523692225, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506315088423065, -99.14624392663488, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50636872096638, -99.14633306721292, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506458632131398, -99.14649279487041, pToken, 10);  -- juan de dios
    CALL arrivedToStop(pToken, 33);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50661694654815, -99.1469721322666, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506642229126548, -99.14743078997438, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506561324861725, -99.14782239246175, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50639951621067, -99.1482649569166, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506161859450078, -99.14878262334634, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505979824256674, -99.14927883197755, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505787675761997, -99.14979649838223, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505557602934807, -99.15037853771993, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50538820844806, -99.15084524205416, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505264322815595, -99.15114564944172, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505150550212402, -99.15142459915873, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504937600450354, -99.1513530186115, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50467569237493, -99.15124017042825, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504510323573342, -99.15115861627939, pToken, 10);  -- eje central lazaro cardenas av o de miguel medizabal ote
    CALL arrivedToStop(pToken, 32);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503747711550478, -99.15085611673553, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503250707914756, -99.15064748985075, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50281806321891, -99.15046541547859, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502431900033788, -99.15033644613163, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501999253140742, -99.15016195818326, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50164169284689, -99.1500367820415, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501319887916654, -99.14988505339802, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501012384829703, -99.14976367048324, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500730499576434, -99.14964539048643, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50053455232964, -99.14959711072773, pToken, 10);   -- politecnico oriente
    CALL arrivedToStop(pToken, 31);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.4999760152608, -99.14935502268723, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49929566490239, -99.14908436751368, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49873437370312, -99.1488949088922, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498309151801376, -99.14871447210983, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49783290194481, -99.14850696981011, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497356650646235, -99.14835359835973, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49674432554039, -99.1480919650253, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49600442961291, -99.14781228801264, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49527303488521, -99.1474965235061, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494686215816422, -99.14727999936726, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49435453453202, -99.14707249706753, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.493692181068052, -99.1467291042189, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.493637305815664, -99.14662145178283, pToken, 10);  -- Montevideo
    CALL arrivedToStop(pToken, 30);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.493686484833287, -99.14635752375631, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49380026549778, -99.14621402558456, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49389824322997, -99.1461241715813, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.4940619607287, -99.1461094194245, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494210507350264, -99.14617312188344, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494342618911656, -99.14623347158904, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49452782776085, -99.14630119735894, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494699072238745, -99.14636458852134, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49487249651522, -99.1464290731948, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495072738747627, -99.14652580020503, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495278830327386, -99.14657895767319, pToken, 10);  -- eje central lazado cardenas cda de Otavalo
    CALL arrivedToStop(pToken, 29);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495496760777623, -99.14668270883352, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49584544888325, -99.146786459993, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496068693926862, -99.14687780612363, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496282371037047, -99.14697930183428, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49649392172245, -99.14707515887491, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49667676933205, -99.14713492856185, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49686067980011, -99.1472116141976, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496953166493395, -99.14725785112806, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497175346938246, -99.14733679222033, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497365635213736, -99.14741460558533, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497531472860206, -99.14749354668413, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497668607708047, -99.14755218864178, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497804679380195, -99.14760406422235, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49796307516523, -99.14766496163683, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498114029396245, -99.14773262543842, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49835002797818, -99.14781833291303, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49851799072423, -99.1478781026075, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498683827187115, -99.14793336137805, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49885391569439, -99.1480089192851, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499001679934466, -99.148040495722, pToken, 10);    -- eje central lazaro cardenas av Wilfrido Massieu
    CALL arrivedToStop(pToken, 28);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49925574907215, -99.14815101325381, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49942158478019, -99.14822431570343, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499568285458363, -99.14826491398487, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499719238190384, -99.14833370551212, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49988613646982, -99.14839685839384, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500177410757566, -99.14849609862789, pToken, 10);  -- eje central politecnico oriente
    CALL arrivedToStop(pToken, 27);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500384704185468, -99.14860436070497, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500521836615984, -99.14865398082465, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50067491453597, -99.14870247320991, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500795038080078, -99.14875773197451, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500934296241976, -99.1488242680549, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501042726173832, -99.14886373860102, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501172416786815, -99.14892012510177, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501302107290215, -99.14897312840658, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501430734658314, -99.14901823760913, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50155404675, -99.14907124091613, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50172944740448, -99.14913213833347, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50192569823236, -99.149218241452, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502092857469513, -99.1492713464772, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50227431890888, -99.14935479723573, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50239767681693, -99.14939557431315, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502521928528072, -99.14944773104139, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502683723776993, -99.14950368098616, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502882168555587, -99.14958428683174, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502967982441888, -99.14961747747688, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50309402150133, -99.14967153080934, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503228105499254, -99.1497085146726, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50340599009675, -99.14978817221599, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503554376093636, -99.1498573984116, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503697395088025, -99.14992286789581, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50391531419942, -99.14999391488585, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50422465252759, -99.15011796517746, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504407491402635, -99.15020367265633, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504586078010433, -99.15028148602656, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504725332903394, -99.15030178516457, pToken, 10);  -- eje central - av miguel o de mendizabal ote
    CALL arrivedToStop(pToken, 26);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.5048762808248, -99.15038862036744, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505028291616235, -99.15045515643398, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505152663972595, -99.15050364881924, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505216444631085, -99.15045966735354, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505249397961435, -99.1503299784162, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505311052563556, -99.15016307438474, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50536951811087, -99.14998602079203, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505413101505123, -99.14983264952703, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505486449144556, -99.1496206362883, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505511961359872, -99.14942666673603, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505523654456507, -99.14923382492489, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505572552854478, -99.14910526369299, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50562906579751, -99.14893021949379, pToken, 10);   -- Neptuno
    CALL arrivedToStop(pToken, 25);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50570301778158, -99.1487384415512, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505747894610746, -99.14863316484319, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505799092103, -99.14847156176194, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505872411943045, -99.14829855926843, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505941939346485, -99.14811818070818, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506014627053794, -99.14792975552041, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.5061138615232, -99.1476635462789, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50618781328485, -99.14748786159248, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506252916090137, -99.14731284744991, pToken, 10);  -- av miguel - juan de dios
    CALL arrivedToStop(pToken, 24);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506299688962358, -99.14711034068421, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506282623186816, -99.14685016640297, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506248491627773, -99.14666174120467, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506164426832886, -99.14645051724969, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.506070880994606, -99.14630098409206, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50594446761483, -99.14612664050914, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.5058414406372, -99.14597911901201, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50575421528953, -99.14583293861035, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505690376418684, -99.14574710792142, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505593037897288, -99.14560159809308, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505464447690894, -99.14545058043636, pToken, 10);  -- escom
    CALL arrivedToStop(pToken, 23);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505369637310594, -99.14523533315146, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505241959243353, -99.14498856991789, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505183808802823, -99.14476326436663, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505119337638074, -99.1446003201675, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505085837905416, -99.14447157414426, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505052338164994, -99.14428381949847, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.505016310134533, -99.14402364522198, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504973961387755, -99.14382046790405, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504920235348536, -99.14363472492279, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504857028222172, -99.14338863226378, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504834273648463, -99.14321764142424, pToken, 10);  -- r ma luisa stampa
    CALL arrivedToStop(pToken, 22);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50480330214271, -99.14307816654102, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50477169855977, -99.14293668002591, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504754632622415, -99.14280860455489, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504738830826977, -99.14267851742723, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504700274436992, -99.14246125847131, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50466551047327, -99.14228087992835, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50466084678318, -99.14222598533486, pToken, 10);   -- central de inteligencia
    CALL arrivedToStop(pToken, 21);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50462165830412, -99.14198860983011, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504581837743412, -99.14177135089774, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50453885681026, -99.14153397541739, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504502828665302, -99.14133012750605, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504485762699588, -99.14110482196537, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504437725155483, -99.14088286917028, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504394744183504, -99.14061598936752, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50433596063653, -99.14032622928785, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504301383987087, -99.14008700165213, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504277330661377, -99.1399418702198, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504250270665683, -99.13981747184921, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504244257332697, -99.13972975504943, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504221707331972, -99.13961173608249, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504178110655012, -99.13948574286101, pToken, 10);  -- cancha de entrenamiento
    CALL arrivedToStop(pToken, 20);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50416307731035, -99.13915879831391, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504099937268734, -99.13890840672187, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504039803872868, -99.13859741079541, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.504008233831076, -99.13835658830878, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50396614043246, -99.13818593926197, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503933067040137, -99.1379977468552, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503883456938976, -99.13774097585953, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503845873515473, -99.13748739449049, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.5038188134476, -99.13730079693464, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503781230012482, -99.13715088094959, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503758679947218, -99.13703286198262, pToken, 10);  -- fte esia 11
    CALL arrivedToStop(pToken, 19);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503724103171773, -99.13684147979673, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503686519714662, -99.13662617492459, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503654949603927, -99.13643798251782, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50363540619896, -99.13631996355085, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503611352774225, -99.13614293510042, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503567755932767, -99.13597707060632, pToken, 10);  -- Manuel de anda y barredo
    CALL arrivedToStop(pToken, 18);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50351363569037, -99.1357506017306, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503388858416756, -99.13565172097451, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503214470740573, -99.13566129007995, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.503035572832374, -99.13570435105468, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50282059442477, -99.13570594590558, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.5026988233119, -99.13571073045829, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502598098988788, -99.13572667896734, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50250639469977, -99.13573465322186, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502369589844104, -99.13575698113453, pToken, 10);  -- fte edif 8 esiqie
    CALL arrivedToStop(pToken, 17);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.502232581323483, -99.13579467139496, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50206037031137, -99.13582399237208, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.5019859580824, -99.13584541924743, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501838196567792, -99.13588150660391, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50167874154452, -99.13590857212125, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501505466898053, -99.13594578721757, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501366209233353, -99.13595593678656, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501251401297512, -99.13598413003383, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501181240852155, -99.13598864095337, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.501076000127085, -99.1359987905224, pToken, 10);   -- fte edif 6 esiqie
    CALL arrivedToStop(pToken, 16);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50087191529802, -99.13605808981953, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500680151089426, -99.13608236064725, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500572882208523, -99.13611270637439, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.50044415946287, -99.13613546567092, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500344041701013, -99.13616012157547, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500224257868805, -99.13617339783177, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.500109837408964, -99.13619995034438, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49995787261064, -99.13622081303285, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499888204453434, -99.1362450664133, pToken, 10);   -- fte edif 4 esime
    CALL arrivedToStop(pToken, 15);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.4996717184612, -99.13628972225656, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49948529975874, -99.13632161927465, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499389083570215, -99.13633118838007, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49921670730844, -99.13636204699995, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499029610367057, -99.13640941165532, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498793612780176, -99.13644549901178, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498581002046823, -99.13649286366716, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498536353757316, -99.13649511912693, pToken, 10);  -- fte edif 2 esime
    CALL arrivedToStop(pToken, 14);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498376895461302, -99.1365176737347, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498236572047244, -99.13655150563139, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.4980941223964, -99.13658082660852, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49796017708791, -99.13661916942476, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49778583540923, -99.13664172402257, pToken, 10);   -- fte edif 1 esime 
    CALL arrivedToStop(pToken, 13);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497555827462417, -99.13668917993616, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49733471060253, -99.13673880005132, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497117845696444, -99.1367748874078, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496900980499746, -99.13681999660338, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496621285086253, -99.1368679858484, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496328075967337, -99.13693247052187, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496059896429998, -99.13701212805968, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.4957988679202, -99.13709557881361, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495587899364914, -99.13716006348707, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495484202845105, -99.13725110069404, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495541414720538, -99.1375280054684, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495605778056227, -99.1378124966749, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495677292843645, -99.1381538861227, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495748807599462, -99.13841182481661, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49581674658822, -99.13866976351049, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49588110981441, -99.13895046150093, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495938321549538, -99.13929185094874, pToken, 10);  -- cenlex
    CALL arrivedToStop(pToken, 12);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496038442040007, -99.13964462011569, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496088502260672, -99.1398418673522, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49615644110683, -99.13999738921176, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496224379924445, -99.14021360252872, pToken, 10);  -- planetario
    CALL arrivedToStop(pToken, 11);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496406741883483, -99.1405322327687, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496503286361214, -99.14075603251783, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496621285089077, -99.14096845261868, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496678496562538, -99.14109742196565, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496767889449327, -99.14136294709171, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496846555148842, -99.14152605538344, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496950250798758, -99.14175364844861, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497000310737373, -99.14190158387598, pToken, 10);  -- centro de formación
    CALL arrivedToStop(pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49720770175075, -99.1423074580352, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497350729878168, -99.14257298316127, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49747230368707, -99.14284988793563, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497508060672313, -99.14297506406648, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49763321005842, -99.14324058919256, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497786964886, -99.14355163291165, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497947870950696, -99.1438740563226, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498058717259596, -99.14416613403216, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498194593271236, -99.14454924887924, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498312590769114, -99.14483374011728, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498477071982713, -99.14524720068358, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498625769252076, -99.14553770455744, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498693707033013, -99.14579943646741, pToken, 10);  -- esc nacional de ciencias
    CALL arrivedToStop(pToken, 9);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498890368869596, -99.14617117164391, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498954730873294, -99.14640255782521, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49905127383083, -99.14659980506173, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.499147816730776, -99.14679325908214, pToken, 10);  -- cda Manuel stampa
    CALL arrivedToStop(pToken, 8);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49906200082257, -99.14691084878085, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498901095871982, -99.14697154023823, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49882243117124, -99.14683119124302, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498758069114924, -99.14657704576521, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498597163862122, -99.14628496812652, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49853995306701, -99.14611427340263, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498407653025833, -99.14595875154306, pToken, 10);  -- Salvatierra
    CALL arrivedToStop(pToken, 7);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49833256375168, -99.14557563668095, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498171658075748, -99.14524562688139, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.498082265964428, -99.14498010175532, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497907057282987, -99.14463112587532, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497771181028227, -99.14431249568585, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49769808363938, -99.14415723307971, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497624417311535, -99.14396744582203, pToken, 10);  -- via ceti
    CALL arrivedToStop(pToken, 6);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497522186425268, -99.14374257179338, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497445513224463, -99.14356873304476, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497337268643832, -99.1433502384708, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.497220003597548, -99.143085493175, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49710123506885, -99.14283510158295, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.4970155412584, -99.14261660696161, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496872718151277, -99.14233591318866, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496805065057416, -99.1421828075018, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496735908532212, -99.14204565032398, pToken, 10);  -- fte centro de formación
    CALL arrivedToStop(pToken, 5);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496653221339944, -99.14187978580858, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496567527297376, -99.14167086029988, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496412676548758, -99.14132796735532, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496317961940164, -99.1411142573341, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.496224750683943, -99.14090054731284, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49613304278268, -99.14069800124794, pToken, 10);   -- av 45 metros
    CALL arrivedToStop(pToken, 4);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49602930754373, -99.14052097271984, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49595413704593, -99.14031523695311, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495886483568036, -99.1401126908882, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495817326650304, -99.13995161094682, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495770720884728, -99.13976660824186, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495727121930592, -99.13962466651132, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495691040028625, -99.13951302694801, pToken, 10);  -- fte cenlex
    CALL arrivedToStop(pToken, 3);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495638500358247, -99.13927539779505, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495544949846025, -99.13884686043694, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495481165374876, -99.1385310960678, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495408876277185, -99.13829652825072, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49538336247028, -99.13810706962924, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49529831641824, -99.13780032709921, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495264297984907, -99.1375477156039, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495183504177096, -99.13734923514329, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495136728796243, -99.13703347077417, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49508144878326, -99.1368530339918, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49503042106221, -99.13659591157693, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494949627137636, -99.13634781100117, pToken, 10);  -- oroya
    CALL arrivedToStop(pToken, 2);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49486458084902, -99.13598693731782, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494796543792873, -99.1356801947878, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494707245113283, -99.13529676662526, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494634955669902, -99.13494491489965, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49459243245283, -99.1346742597261, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494694488155027, -99.1345208884611, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49482205769229, -99.13451186662198, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.494881590108587, -99.13462463961096, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49496238406713, -99.13484116374981, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49496238406713, -99.13497649133657, pToken, 10);   -- el queso
    CALL arrivedToStop(pToken, 1);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495051682605947, -99.13535540857954, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.495098458011377, -99.13556742179881, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49517499955475, -99.13567568386823, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49525579336681, -99.1357794350181, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49531107332025, -99.13591025168532, pToken, 10);
    SET @test_trash = (SELECT SLEEP(1));
    CALL reportLocation(19.49528981180192, -99.13602753559384, pToken, 10);   -- terminal zacatenco
    CALL arrivedToStop(pToken, 46);
    SET @test_trash = (SELECT SLEEP(1));
    CALL endTrip(pToken);

END$$


DELIMITER ;