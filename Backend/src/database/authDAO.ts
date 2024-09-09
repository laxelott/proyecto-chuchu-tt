import { PoolConnection } from 'promise-mysql';
import SQLPool from './conn/databaseConnection.js';
import { FieldPacket, QueryResult, RowDataPacket } from 'mysql2';

export class AuthDAO {
    static async testData() {
        const results = await SQLPool.query(
            "CALL testData()"
        );
        return results;
    }

    static async authorizeDriver(username: string, password: string) {
        const results: any = await SQLPool.query(
            "CALL authorizeDriver(?, ?)",
            [username, password]
        );

        return results;
    }
}