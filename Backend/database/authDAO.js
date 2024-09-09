import { dbPool } from './databaseConnection.js';

export class AuthDAO {
    async authorizeDriver(username, password) {
        const result  = (await dbPool).query(
            "CALL authorizeDriver(?, ?)",
            [username, password]
        );
        return result[0][0].auth;
    }

    async authorizeDriver(username, password) {
        const result  = (await dbPool).query(
            "CALL testData()"
        );
        return result[0][0].test;
    }
}