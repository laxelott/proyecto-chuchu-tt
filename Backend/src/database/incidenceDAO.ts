import SQLPool from './conn/databaseConnection.js';
import crypto from 'crypto';

export class AuthDAO {
    static async authorizeDriver(username: string, password: string) {
        const salt: any = (await SQLPool.query(
            "CALL findDriverSalt(?)",
            [username]
        ))[0].salt;
    }
}