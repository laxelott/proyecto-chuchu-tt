import { dbPool } from './conn/databaseConnection.js';
import { RowDataPacket } from 'mysql2';

export class AuthDAO {
    static async authorizeDriver(username: String, password: String) {
        const result: any = (await dbPool).query<RowDataPacket[]>(
            "CALL authorizeDriver(?, ?)",
            [username, password]
        );
        return result[0][0].auth;
    }

    static async testData() {
        const result: any = (await dbPool).query<RowDataPacket>(
            "CALL testData()"
        );
        return result[0][0].test;
    }
}