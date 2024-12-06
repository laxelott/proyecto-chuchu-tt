import SQLPool from './conn/databaseConnection.js';
import crypto from 'crypto';

export class DriverDAO {
    static async startTrip(token: any, routeId: any) {
        const results: any = (await SQLPool.query(
            "CALL startTrip(?, ?)",
            [token, routeId]
        ));

        return results[0];
    }
    static async endTrip(token: any) {
        const results: any = (await SQLPool.query(
            "CALL endTrip(?)",
            [token]
        ));

        return results[0];
    }
    static async cancelTrip(token: any, reason: any) {
        const results: any = (await SQLPool.query(
            "CALL cancelTrip(?, ?)",
            [token, reason]
        ));

        return results[0];
    }
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

    static async arrivedToStop(token: any, idStop: string) {
        const results: any = (await SQLPool.query(
            "CALL arrivedToStop(?)",
            [token, idStop]
        ));

        return results[0];
    }

    static async getWaitTime(token: any) {
        const results: any = (await SQLPool.query(
            "CALL getWaitTimeWithToken(?)",
            [token]
        ));

        return results[0];
    }
}