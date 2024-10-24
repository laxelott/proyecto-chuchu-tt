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
    
    static async useVehicle(vehicleIdentifier: string, token: any) {
        const results: any = (await SQLPool.query(
            "CALL useVehicle(?,?)",
            [vehicleIdentifier, token]
        ));

        return results[0];
    }

    static async leaveVehicle(token: any) {
        const results: any = (await SQLPool.query(
            "CALL leaveVehicle(?)",
            [token]
        ));

        return results[0];
    }
}