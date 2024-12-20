import SQLPool from './conn/databaseConnection.js';
import crypto from 'crypto';

export class AuthDAO {
    static async testData() {
        const results = await SQLPool.query(
            "CALL testData()"
        );
        return results;
    }

    static async loginDriver(username: string, password: string) {
        const salt: any = (await SQLPool.query(
            "CALL findDriverSalt(?)",
            [username]
        ))[0].salt;

        if (salt == "not-found") {
            throw Error("Usuario no encontrado!");
        } else {
            const saltedKey = await AuthDAO.hashKey(password, salt);

            const results: any = await SQLPool.query(
                "CALL loginDriver(?, ?)",
                [username, saltedKey]
            );

            return results;
        }
    }

    static async loginAdmin(username: string, password: string) {
        const salt: any = (await SQLPool.query(
            "CALL findAdminSalt(?)",
            [username]
        ))[0].salt;

        if (salt == "not-found") {
            throw Error("Usuario no encontrado!");
        } else {
            const saltedKey = await AuthDAO.hashKey(password, salt);

            const results: any = await SQLPool.query(
                "CALL loginAdmin(?, ?)",
                [username, saltedKey]
            );

            return results;
        }
    }


    static async logoutDriver(token: string) {
        const results: any = await SQLPool.query(
            "CALL logoutDriver(?)",
            [token]
        );

        await SQLPool.query(
            "CALL setDriverInactive(?)",
            [token]
        )

        return results;
    }

    static async getHashedPassword(username: string, password: string) {
        const salt: any = (await SQLPool.query(
            "CALL findDriverSalt(?)",
            [username]
        ))[0].salt;

        if (salt == "not-found") {
            throw Error("Usuario no encontrado!");
        } else {
            const saltedKey = await AuthDAO.hashKey(password, salt);

            return saltedKey;
        }
    }

    static hashKey(key: string, salt: string) {
        return new Promise((resolve, reject) => {
            crypto.pbkdf2(key, salt, 100000, 50, 'sha512', async (err, derivedKey) => {
                if (err) {
                    reject(err);
                } else {
                    resolve(derivedKey.toString('hex'));
                }
            })
        }) 
    }

    static async logoutAdmin(token: string) {
        const results: any = await SQLPool.query(
            "CALL logoutAdmin(?)",
            [token]
        );
    
        return results[0];
    }

    static async checkToken(token: string, username: string) {
        const results: any = await SQLPool.query(
            "CALL checkToken(?,?)",
            [token, username]
        );
    
        return results[0];
    }

    static async checkVehicle(token: string, identifier: string) {
        const results: any = await SQLPool.query(
            "CALL checkVehicle(?,?)",
            [token, identifier]
        );
    
        return results[0];
    }
    
    static async forgotPassword(username: any) {
        const results: any = await SQLPool.query(
            "CALL forgotPassword(?)",
            [username]
        );
    
        return results[0];
    }
}