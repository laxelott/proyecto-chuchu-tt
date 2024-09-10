import SQLPool from './conn/databaseConnection.js';
import crypto from 'crypto';

export class AuthDAO {
    static async testData() {
        const results = await SQLPool.query(
            "CALL testData()"
        );
        return results;
    }

    static async authorizeDriver(username: string, password: string) {
        const salt: any = (await SQLPool.query(
            "CALL findDriverSalt(?)",
            [username]
        ))[0];

        if (salt == "not-found") {
            throw Error("Usuario no encontrado!");
        } else {
            const saltedKey = await AuthDAO.hashKey(password, salt);

            const results: any = await SQLPool.query(
                "CALL authorizeDriver(?, ?)",
                [username, saltedKey]
            );

            return results;
        }
    }

    static async getHashedPassword(username: string, password: string) {
        const salt: any = (await SQLPool.query(
            "CALL findDriverSalt(?)",
            [username]
        ))[0];

        if (salt == "not-found") {
            throw Error("Usuario no encontrado!");
        } else {
            const saltedKey = await AuthDAO.hashKey(password, salt);

            return saltedKey;
        }
    }

    static hashKey(key: string, salt: string) {
        return new Promise((resolve, reject) => {
            crypto.pbkdf2(key, salt, 100000, 200, 'sha512', async (err, derivedKey) => {
                if (err) {
                    reject(err);
                } else {
                    resolve(derivedKey);
                }
            })
        }) 
    }
}