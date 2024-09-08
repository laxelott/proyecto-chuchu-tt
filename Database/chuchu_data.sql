DELIMITER $$

-- -----------------------------------------------------------------------------
  -- insertData
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS insertData$$
CREATE PROCEDURE insertData()
BEGIN

    CALL upsertUser(
        0,
        'Juan Gómez',
        '5b87d86a52bfe61c36132387f7fb73bcfd7fed18495dbf6f7f9183da79dbf0ae',
        '5512345678',
        'correo@falso.com',
        'JUNN0190892HNM'
    );

    IF (SELECT COUNT(*) FROM transportation WHERE idTransportation = 0) = 0 THEN
        INSERT INTO transportation(idTransportation, name) VALUES (
            0, 'Trolebus'
        );
    END IF;

    IF (SELECT COUNT(*) FROM line WHERE idLine = 0) = 0 THEN
        INSERT INTO line(0, 0, 'Linea 8', 'Circuito Politécnico');
    END IF;

    IF (SELECT COUNT(*) FROM stop WHERE idLine = 0) = 0 THEN
        -- TODO insert test stops (from last to first)
        CALL upsertStop(0, 'paradaFinal', 0, 0, NULL);
    END IF;

END$$

CALL insertData()$$