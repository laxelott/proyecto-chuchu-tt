DELIMITER $$

-- -----------------------------------------------------------------------------
  -- authorizeUser
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS authorizeUser$$
CREATE PROCEDURE authorizeUser(
    userName DATETIME,
    password DATETIME
)
BEGIN

    SELECT count(*) FROM usuario
        WHERE usuario.nomUsuario = userName
        AND usuario.contrasena = CONCAT(usuario.salt, password);

END$$