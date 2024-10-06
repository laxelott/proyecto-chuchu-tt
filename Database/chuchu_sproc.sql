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

    select 0 as test;
    IF (SELECT token FROM Driver d WHERE d.username = pUsername) IS NOT NULL THEN
        SELECT 2 INTO login;
    ELSE
        select 1 as test;
        SELECT count(*) AS auth FROM Driver d
            WHERE d.username = pUsername
            AND d.password = pPassword
            INTO login;
        
        select 2 as test;
        UPDATE Driver d
            SET d.token = newToken;

    END IF;
   
    select 3 as test;
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
            s.coordX,
            s.coordY,
            s.iconB64
        FROM Stop s
        WHERE idStop = nextStopId;

        UPDATE temp_StopsInRoute
        SET waypoints = (
            SELECT
                GROUP_CONCAT(CONCAT(w.coordX, ',', w.coordY) SEPARATOR '|') as waypoints
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
        v.identifier AS 'vehicleIdentifier',
        CONCAT(r.name, ' - ', r.description) AS 'routeName',
        r.color AS 'routeColor',
        r.iconB64 AS 'routeIcon'
    FROM Driver d
        INNER JOIN Driver_Vehicle dv ON d.idDriver = d.idDriver
        INNER JOIN Vehicle v ON dv.idVehicle = v.idVehicle
        INNER JOIN Vehicle_Route vr ON v.idVehicle = vr.idVehicle
        INNER JOIN Route r ON vr.idRoute = r.idRoute
    WHERE d.token = pToken;

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
  -- setDriverInactive
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS setDriverInactive$$
CREATE PROCEDURE setDriverInactive(
    `pToken` varchar(40)
)
BEGIN

    IF (SELECT COUNT(*) FROM Vehicle v WHERE v.token = pToken) > 0 THEN
        UPDATE Vehicle SET
            token = NULL WHERE token = pToken;
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
    `pCoordX` double,
    `pCoordY` double,
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
            s.coordX = pCoordX,
            s.coordY = pCoordY,
            s.idNext = -1
        WHERE s.idStop = vCurrIdStop;
    ELSE
        INSERT INTO Stop(
            idRoute, name, coordX, coordY, idNext
        ) VALUES (
            pIdRoute, pName, pCoordX, pCoordY, -1
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

DELIMITER ;