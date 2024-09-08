DELIMITER $$

-- -----------------------------------------------------------------------------
  -- testData
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS testData$$
CREATE PROCEDURE testData()
BEGIN

    IF (SELECT COUNT(*) FROM user WHERE idUser = 0) > 0 THEN
        SELECT CONCAT(name, ' (', curp, '), ', email) FROM user WHERE idUser = 0;
    ELSE
        SELECT 'Funcionando, pero no se encuentra el usuario :c';
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- authorizeUser
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS authorizeUser$$
CREATE PROCEDURE authorizeUser(
    username DATETIME,
    password DATETIME
)
BEGIN

    SELECT count(*) FROM user
        WHERE usuario.username = username
        AND usuario.pass = CONCAT(usuario.salt, password);

END$$

-- -----------------------------------------------------------------------------
  -- getStopsFromLine
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS getStopsFromLine$$
CREATE PROCEDURE getStopsFromLine(
    pIdLine INT
)
BEGIN

    DECLARE nextStopId INT;

    DROP TABLE IF EXISTS temp_StopsInLine;
    CREATE TEMPORARY TABLE temp_StopsInLine(
        idStop INT,
        name VARCHAR(200),
        coordsX DOUBLE,
        coordsY DOUBLE
    );

    -- Selecciona la única parada no referenciada
    SELECT idNext FROM stop WHERE idLine = pIdLine AND idStop NOT IN (
        SELECT idNext FROM stop WHERE idLinea = pIdLine
    ) INTO nextStopId;

    WHILE nextStopId = NULL DO
        INSERT INTO temp_StopsInLine
        SELECT
            idStop,
            name,
            coordsX,
            coordsY
        FROM stop
        WHERE idStop = nextStopId;

        SELECT idNext FROM stop WHERE idStop = nextStopId
        INTO nextStopId;
    END WHILE;

    SELECT * FROM temp_StopsInLine;
    DROP TABLE temp_StopsInLine;

END$$

-- -----------------------------------------------------------------------------
--                 UPSERTS
-- -----------------------------------------------------------------------------

-- -----------------------------------------------------------------------------
  -- upsertUser
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertUser$$
CREATE PROCEDURE upsertUser(
    pIdUserType INT,
    pName VARCHAR(200),
    pPassword VARCHAR(200),
    pPhone VARCHAR(30),
    pEmail VARCHAR(100),
    pCurp VARCHAR(20)
)
BEGIN

    DECLARE newUsername VARCHAR(50);
    DECLARE newSalt VARCHAR(35);

    -- TODO newUsername generation logic
    SET newUsername = pCurp;

    -- salt random generation logic
    SET newSalt = MD5(RAND());

    WHILE (SELECT COUNT(*) FROM user WHERE salt = newSalt) > 0 DO
        SET newSalt = MD5(RAND());
    END WHILE;

    IF (SELECT COUNT(*) FROM user WHERE curp = pCurp) > 0 THEN
        -- update
        UPDATE user SET
            idUserType = pIdUserType,
            name = pName,
            username = newUsername,
            password = pPassword,
            salt = newSalt,
            phone = pPhone,
            email = pEmail
        WHERE curp = pCurp;
        SELECT idUser FROM user WHERE curp = pCurp;
    ELSE 
        -- insert
        INSERT INTO user(
            idUserType, name, username, password, salt, phone, email, curp
        ) VALUES (
            pIdUserType, pName, newUsername, pPassword, newSalt, pPhone, pEmail, pCurp
        );
        SELECT LAST_INSERT_ID();
    END IF;

END$$

-- -----------------------------------------------------------------------------
  -- upsertStop
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertStop$$
CREATE PROCEDURE upsertStop(
    pIdStop INT,
    pIdLine INT,
    pName VARCHAR(200),
    pCoordsX DOUBLE,
    pCorodsY DOUBLE,
    pIdNext INT
)
BEGIN

    DECLARE newIdNext INT;

    IF (SELECT COUNT(*) FROM stop WHERE idStop = pIdStop) > 0 THEN
        -- update
        -- remove stop from chain
        UPDATE stop SET
            idNext = (SELECT idNext FROM stop WHERE idStop = pIdStop);
        
        UPDATE stop SET
            idLine = pIdLine,
            name = pName,
            coordsX = pCoordsX,
            coordsY = pCorodsY,
            idNext = -1
        WHERE idStop = pIdStop;

        SET newIdNext = pIdStop;
    ELSE 
        -- insert
        INSERT INTO stop(
            idLine, name, coordsX, coordsY, idNext
        ) VALUES (
            pIdLine, pName, pCoordsX, pCorodsY, -1
        );

        SELECT LAST_INSERT_ID() INTO newIdNext;
    END IF;

    -- link into chain
    UPDATE stop SET
        idNext = newIdNext
    WHERE idNext = pIdNext;
    
    UPDATE stop SET
        idNext = pIdNext
    WHERE idStop = newIdNext;

    SELECT newIdNext;

END$$

-- -----------------------------------------------------------------------------
  -- upsertLine
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upsertLine$$
CREATE PROCEDURE upsertLine(
    pIdLine INT,
    pIdTransportation INT,
    pName VARCHAR(200),
    pDescription VARCHAR(200)
)
BEGIN

    IF (SELECT COUNT(*) FROM line WHERE idLine = pIdLine) > 0 THEN
        -- update
        UPDATE line SET
            idTransportation = pIdTransportation,
            name = pName,
            description = pDescription
        WHERE idLine = pIdLine;
        SELECT pIdLine;
    ELSE
        -- insert
        INSERT INTO line(
            idTransportation, name, description
        ) VALUES (
            pIdTransportation, pName, pDescription
        );
        SELECT LAST_INSERT_ID();
    END IF;
END$$
