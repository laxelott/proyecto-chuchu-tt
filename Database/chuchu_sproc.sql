USE Chuchu;

DELIMITER $$

-- -----------------------------------------------------------------------------
  -- testData
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS testData$$
CREATE PROCEDURE testData()
BEGIN

    IF (SELECT COUNT(*) FROM Driver) > 0 THEN
        SELECT ( CONCAT(d.name, ' (', d.curp, ')') ) AS 'test' FROM Driver d LIMIT 1;
    ELSE
        SELECT 'Funcionando, pero no hay conductores :c' AS 'test';
    END IF;

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
        SELECT 2 INTO login;
    ELSE
        SELECT count(*) AS auth FROM Driver d
            WHERE d.username = pUsername
            AND d.password = pPassword
            INTO login;
        
        UPDATE Driver d
            SET d.token = newToken
            WHERE d.username = pUsername;

    END IF;
   
    IF login = 1 THEN
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
        `idStop` int,
        `name` varchar(200),
        `latitude` double,
        `longitude` double,
        `waypoints` varchar(1000),
        `icon` blob
    );

    -- Get stop that's not linked to (start of route)
    SELECT idStop FROM Stop WHERE idRoute = pIdRoute AND idStop NOT IN (
        SELECT idNext FROM Stop WHERE idRoute = pIdRoute and idNext IS NOT NULL
    ) INTO nextStopId;

    -- Fill route with linked stops
    WHILE nextStopId IS NOT NULL DO
        INSERT INTO temp_StopsInRoute(
            `idStop`,
            `name`,
            `latitude`,
            `longitude`,
            `icon`
        )
        SELECT
            s.idStop,
            s.name,
            s.lon,
            s.lat,
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
        WHERE idStop = nextStopId;

        SELECT idNext FROM Stop WHERE idStop = nextStopId
        INTO nextStopId;
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
BEGIN

    SELECT DISTINCT
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
        AND v.driverToken IS NULL;

END$$

-- -----------------------------------------------------------------------------
  -- useVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS useVehicle$$
CREATE PROCEDURE useVehicle(
    `pVehicleIdentifier` varchar(255),
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Vehicle WHERE identifier = pVehicleIdentifier AND driverToken = pToken) > 0 THEN
        SELECT 2 as error, 'vehicle-in-use' as message;
    ELSE
        UPDATE Vehicle
            SET driverToken = pToken
            WHERE identifier = pVehicleIdentifier;
        
        SELECT 0 as error;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- useVehicle
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS useVehicle$$
CREATE PROCEDURE useVehicle(
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Vehicle WHERE driverToken = pToken) = 0 THEN
        SELECT 1 as error, 'vehicle-available' as message;
    ELSE
        UPDATE Vehicle
            SET driverToken = NULL
            WHERE driverToken = pToken;
        
        SELECT 0 as error;
    END IF;

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
        it.name AS name,
        it.idIncidentType AS type,
        i.description AS description,
        i.lon AS lon,
        i.lat AS lat
    FROM Incident i
        INNER JOIN Incident_IncidentType iit ON i.idIncident = iit.idIncident
        INNER JOIN IncidentType it ON iit.idIncidentType = it.idIncidentType
    WHERE i.idRoute = pIdRoute;

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
BEGIN

    DECLARE vNewId INT;

    IF (SELECT COUNT(*) FROM IncidentType it WHERE it.idIncidentType = pIncidentType) > 0 THEN
        IF (SELECT COUNT(*) FROM Driver d WHERE d.token = pToken) > 0 THEN

            INSERT INTO Incident (idRoute, description, lon, lat)
            VALUES (pIdRoute, pDescription, pLon, pLat);

            SET vNewId = LAST_INSERT_ID();

            INSERT INTO Incident_IncidentType(idIncident, idIncidentType)
            VALUES (
                vNewId, pIncidentType
            );
            INSERT INTO Driver_Incident(idIncident, idDriver)
            VALUES (
                vNewId, (SELECT idDriver FROM Driver d WHERE d.token = pToken)
            );

            SELECT 0 AS error, vNewId as `idIncident`;

        ELSE 
            SELECT 2 AS error, 'invalid-token' AS message;
        END IF;
    ELSE
        SELECT 1 AS error, 'invalid-type' AS message;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- removeIncident
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS removeIncident$$
CREATE PROCEDURE removeIncident(
    `pIdIncident` int,
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Incident i WHERE i.idIncident = pIdIncident) > 0 THEN
        IF (SELECT COUNT(*) FROM Driver d WHERE d.token = pToken) > 0 THEN

            DELETE FROM Driver_Incident
                WHERE idIncident = pIdIncident;
            DELETE FROM Incident_IncidentType
                WHERE idIncident = pIdIncident;
            DELETE FROM Incident
                WHERE idIncident = pIdIncident;

            SELECT 0 AS error;
        ELSE 
            SELECT 2 AS error, 'invalid-token' AS message;
        END IF;
    ELSE
        SELECT 1 AS error, 'incident-not-found' AS message;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- setDriverActive
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS setDriverActive$$
CREATE PROCEDURE setDriverActive(
    `pToken` varchar(40),
    `pIdentifier` varchar(40)
)
BEGIN

    DECLARE error int;

    IF (SELECT COUNT(*) FROM Driver d WHERE d.token = pToken) > 0 THEN
        IF (SELECT active FROM Driver d WHERE d.token = pToken) = 0 THEN
            UPDATE Vehicle SET
                token = pToken WHERE identifier = pIdentifier;
            UPDATE Driver SET
                active = 1 WHERE token = pToken;
            SET error = 0;
        ELSE
            SET error = 2;
        END IF;
    ELSE
        SET error = 1;
    END IF;

    SELECT error;

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
BEGIN

    DECLARE vIdVehicle int;
    DECLARE haversineDelta float;
    DECLARE defaultIdStop int;
    SET vIdVehicle = (SELECT idVehicle FROM Vehicle v WHERE v.driverToken = pToken);
    SET defaultIdStop = ( -- First stop in route
        SELECT idStop FROM Stop s
            INNER JOIN Vehicle_Route vr ON s.idRoute = vr.idRoute
            WHERE vr.idVehicle = vIdVehicle AND idStop NOT IN (
                SELECT idNext FROM Stop WHERE idRoute = vr.idRoute and idNext IS NOT NULL
            )
    );

    IF vIdVehicle IS NOT NULL THEN
        IF (SELECT COUNT(*) FROM Last_Location ll WHERE ll.idVehicle = vIdVehicle) = 0 THEN
            INSERT INTO Last_Location(
                idVehicle, idLastStop, lon, lat
            ) VALUES (
                vIdVehicle, defaultIdStop, pLon, pLat
            );

            INSERT INTO VehicleData (
                idVehicle, direction, distanceToStop, avgSpeed
            ) VALUES (
                vIdVehicle,
                0,
                (SELECT distanceTo FROM Stop WHERE idStop = (SELECT idNext FROM Stop WHERE idStop = defaultIdStop)),
                0
            );
        ELSE
            SET @R = 6371000;
            SET haversineDelta = (
                SELECT
                    @R * 2 * ASIN(SQRT(
                        POWER(SIN(RADIANS(pLat - ll.lat) / 2), 2) +
                        COS(RADIANS(ll.lat)) * COS(RADIANS(pLat)) * 
                        POWER(SIN(RADIANS(pLon - ll.lon) / 2), 2)
                    ))
                FROM Last_Location ll
            );

            -- DEBUG
            -- SELECT haversineDelta, pLon, lon, plat, lat,
            --             (
            --                 SELECT
            --                     CASE 
            --                         WHEN (pLon - lon) = 0 THEN 0
            --                         ELSE (pLat-lat)/(pLon-lon)
            --                     END
            --                 FROM Last_Location
            --                     WHERE idVehicle = vIdVehicle
            --             ) as test
            --     FROM Last_Location ll
            --     INNER JOIN VehicleData vd ON vd.idVehicle = ll.idVehicle
            --     WHERE vd.idVehicle = vIdVehicle;
            
            UPDATE VehicleData SET
                direction = (
                    SELECT
                        CASE 
                            WHEN (pLon - lon) = 0 THEN 0
                            ELSE (pLat-lat)/(pLon-lon)
                        END
                    FROM Last_Location
                        WHERE idVehicle = vIdVehicle
                ),
                distanceToStop = GREATEST(distanceToStop - haversineDelta, 0)
                WHERE idVehicle = vIdVehicle;
            UPDATE Last_Location SET
                lon = pLon,
                lat = pLat
                WHERE idVehicle = vIdVehicle;
            
        END IF;
        
        -- alpha for the EWMA (Exponentially Weighted Moving Average)
        SET @alpha = 0.3;
        UPDATE VehicleData SET
            avgSpeed = @alpha * pSpeed + (1 - @alpha) * avgSpeed
            WHERE idVehicle = vIdVehicle;
        
        SELECT 0 AS error, avgSpeed as avgSpeed
            FROM VehicleData;
    ELSE
        SELECT 2 AS error, 'invalid-token' AS message;
    END IF;

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
        vd.direction as direction
    FROM Vehicle v
        INNER JOIN Last_Location ll ON v.idVehicle = ll.idVehicle
        INNER JOIN Vehicle_Route vr ON v.idVehicle = vr.idVehicle
        INNER JOIN VehicleData vd ON v.idVehicle = vd.idVehicle
        WHERE vr.idRoute = pIdRoute;

END$$

-- -----------------------------------------------------------------------------
  -- arrivedToStop
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS arrivedToStop$$
CREATE PROCEDURE arrivedToStop(
    `pToken` int,
    `pIdStop` int
)
BEGIN

    DECLARE vIdVehicle INT;

    SET vIdVehicle = (SELECT idVehicle FROM Vehicle WHERE driverToken = pToken);

    IF vIdVehicle IS NOT NULL THEN
        UPDATE Last_Location SET
            idLastStop = pIdStop,
            lon = (SELECT lon FROM Stop WHERE idStop = pIdStop),
            lat = (SELECT lat FROM Stop WHERE idStop = pIdStop)
            WHERE idVehicle = vIdVehicle;
        UPDATE VehicleData SET
            distanceToStop = (SELECT distanceTo FROM Stop WHERE idStop = (SELECT idNext FROM Stop WHERE idStop = pIdStop))
            WHERE idVehicle = vIdVehicle;


        SELECT 0 AS error;
    ELSE
        SELECT 2 AS error, 'invalid-token' AS message;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- getWaitTimeForStop
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getWaitTimeForStop$$
CREATE PROCEDURE getWaitTimeForStop(
    `pIdRoute` int,
    `pIdStop` int
)
BEGIN

    DECLARE vCurrStop int;
    DECLARE vTotalDistance float;
    DECLARE vIdVehicle int;

    IF ((SELECT COUNT(*) FROM Stop WHERE idStop = pIdStop) + (SELECT COUNT(*) FROM Route WHERE idRoute = pIdRoute)) = 2 THEN
        SET vCurrStop = pIdStop;
        SET vTotalDistance = 0;

        WHILE (SELECT COUNT(*) FROM Last_Location WHERE idLastStop = vCurrStop) = 0 DO
            SET vTotalDistance = vTotalDistance + (SELECT distanceTo FROM Stop WHERE idStop = vCurrStop);

            SET vCurrStop = (SELECT idStop FROM Stop WHERE idNext = vCurrStop);
            IF vCurrStop IS NULL THEN
                SET vCurrStop = (SELECT idStop FROM Stop WHERE idRoute = pIdRoute AND idNext IS NULL);
            END IF;
        END WHILE;
        
        SET vIdVehicle = (SELECT idVehicle FROM Last_Location WHERE idLastStop = vCurrStop LIMIT 1);
        SET vTotalDistance = vTotalDistance + (SELECT distanceToStop FROM VehicleData WHERE idVehicle = vIdVehicle);

        SELECT
            0 AS error,
            identifier as identifier,
            vTotalDistance as distance,
            (vTotalDistance / vAvgSpeed) AS time,
            avgSpeed AS speed
        FROM Vehicle v
            INNER JOIN VehicleData vd on vd.idVehicle = v.idVehicle
            WHERE v.idVehicle = vIdVehicle;
    ELSE
        SELECT 1 AS error, 'invalid-id' AS message;
    END IF;

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
BEGIN

    DECLARE vAvgSpeed float;
    DECLARE vCurrStop int;
    DECLARE vTotalDistance float;
    DECLARE vIdVehicle int;
    DECLARE count int;

    SET vIdVehicle = (SELECT idVehicle FROM Vehicle WHERE identifier = pIdentifier);

    IF vIdVehicle IS NOT NULL THEN
        IF ((SELECT COUNT(*) FROM Stop WHERE idStop = pIdStop) + (SELECT COUNT(*) FROM Route WHERE idRoute = pIdRoute)) = 2 THEN
            SET vCurrStop = pIdStop;
            SET vTotalDistance = 0;

            WHILE (SELECT COUNT(*) FROM Last_Location WHERE idLastStop = vCurrStop AND idVehicle = vIdVehicle) = 0 DO
                SET vTotalDistance = vTotalDistance + (SELECT distanceTo FROM Stop WHERE idStop = vCurrStop);
                
                SET vCurrStop = (SELECT idStop FROM Stop WHERE idNext = vCurrStop);
                IF vCurrStop IS NULL THEN
                    SET vCurrStop = (SELECT idStop FROM Stop WHERE idRoute = pIdRoute AND idNext IS NULL);
                END IF;
            END WHILE;

            SET vTotalDistance = vTotalDistance + (SELECT distanceToStop FROM VehicleData WHERE idVehicle = vIdVehicle);

            SELECT
                0 AS error,
                identifier as identifier,
                vTotalDistance as distance,
                (vTotalDistance / vAvgSpeed) AS time,
                avgSpeed AS speed
            FROM Vehicle v
                INNER JOIN VehicleData vd on vd.idVehicle = v.idVehicle
                WHERE v.idVehicle = vIdVehicle;
        ELSE
            SELECT 1 AS error, 'invalid-id' AS message;
        END IF;
    ELSE
        SELECT 2 AS error, 'invalid-id' AS message;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- setDriverInactive
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS setDriverInactive$$
CREATE PROCEDURE setDriverInactive(
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Vehicle v WHERE v.driverToken = pToken) > 0 THEN
        DELETE FROM Last_Location
            WHERE idVehicle = (
                SELECT idVehicle FROM Vehicle WHERE driverToken = pToken
            );
        DELETE FROM VehicleData
            WHERE idVehicle = (
                SELECT idVehicle FROM Vehicle WHERE driverToken = pToken
            );
        UPDATE Vehicle SET
            driverToken = NULL WHERE driverToken = pToken;
        UPDATE Driver SET
            active = 0 WHERE token = pToken;
        SELECT 0 AS error;
    ELSE
        SELECT 1 AS error;
    END IF;

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

    SELECT newToken INTO tokenOut;

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

    SELECT idDriver FROM Driver WHERE curp = pCurp
        INTO vIdDriver;
    SELECT idVehicle FROM Vehicle WHERE identifier = pIdentifier
        INTO vIdVehicle;

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
        SELECT idStop FROM Stop s WHERE s.idRoute = pIdRoute AND s.name = pNameNext
            INTO vIdNext;
    ELSE
        SET vIdNext = NULL;
    END IF;

    IF (SELECT COUNT(*) FROM Stop s WHERE s.idRoute = pIdRoute AND s.name = pName) > 0 THEN
        SELECT s.idStop FROM Stop s WHERE s.idRoute = pIdRoute AND s.name = pName
            INTO vCurrIdStop;
        
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

        SELECT LAST_INSERT_ID() INTO vCurrIdStop;
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

    IF (SELECT token FROM Admin d WHERE d.username = pUsername) IS NOT NULL THEN
        SELECT 2 INTO login;
    ELSE
        SELECT count(*) AS auth FROM Admin d
            WHERE d.username = pUsername
            AND d.password = pPassword
            INTO login;
        
        UPDATE Admin d
            SET d.token = newToken
            WHERE d.username = pUsername;

    END IF;
   
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

    IF (SELECT COUNT(*) FROM Admin d WHERE d.username = pUsername) > 0 THEN
        SELECT d.salt as `salt` FROM Admin d WHERE d.username = pUsername;
    ELSE 
        SELECT 'not-found' as `salt`;
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- generateAdminToke
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS generateAdminToken$$
CREATE PROCEDURE generateAdminToken(
    OUT tokenOut varchar(35)
)
BEGIN

    DECLARE `newToken` varchar(35);
    
    SET newToken = MD5(RAND());
    WHILE (
        (SELECT COUNT(*) FROM Admin d WHERE d.token = newToken)
    ) > 0 DO
        SET newToken = MD5(RAND());
    END WHILE;

    SELECT newToken INTO tokenOut;

END$$

-- -----------------------------------------------------------------------------
  -- logoutAdmin
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS logoutAdmin$$
CREATE PROCEDURE logoutAdmin(
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Admin d WHERE d.token = pToken) > 0 THEN
        UPDATE Driver d SET
            d.token = NULL WHERE d.token = pToken;

        SELECT 1 AS logout;
    ELSE
        SELECT 0 AS logout;
    END IF;

END$$


DELIMITER ;