var mysql = require("promise-mysql");

class GeneralDAO {
    generatePool() {
        return new Promise((resolve, reject) => {
            mysql.createPool({
                user: process.env.DB_USER,
                password: process.env.DB_PASS,
                database: process.env.DB_NAME,
                socketPath: `/cloudsql/${process.env.CLOUD_SQL_CONNECTION_NAME}`,
                host: `/cloudsql/${process.env.CLOUD_SQL_CONNECTION_NAME}`
            })
            .then((result) => {
                this.pool = result;
                return resolve();
            })
            .catch((error) => {
                console.log("ERROR: Cannot connect to database!");
                console.log(error);
                return reject();
            })
        })
    }

    authorizeUser(user, pass) {
        return new Promise((resolve, reject) => {
            this.pool.query(
                "CALL authorizeUser(?,?)",
                [user, pass]
            ).then((results) => {
                return resolve(results[0]==1)
            }).catch((error) => {
                return reject (error);
            });
        })
    }

    testDB() {
        return new Promise((resolve, reject) => {
            this.pool.query(
                "SELECT nombre FROM usuario WHERE idUsuario = 0"
            ).then((results) => {
                return resolve(results[0].nombre)
            }).catch((error) => {
                return reject (error);
            });
        })
    }
}

module.exports = new GeneralDAO();