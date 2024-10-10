import SQLPool from './conn/databaseConnection.js';
import crypto from 'crypto';

export class DriverDAO {
    static async getDriverInfo(token: string) {
        const results: any = (await SQLPool.query(
            "CALL getDriverInfo(?)",
            [token]
        ));

        return results;
    }
}