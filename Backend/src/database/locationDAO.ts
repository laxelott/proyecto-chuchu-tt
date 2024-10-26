import SQLPool from './conn/databaseConnection.js';

export class LocationDAO {
    static async writeLocation(lat: number, lon: number) {
        const results: any = await SQLPool.query(
            "INSERT INTO Waypoint(lon, lat) VALUES (?, ?)",
            [lat, lon]
        );

        return results;
    }

    static async reportLocation(lat: number, lon: number, token: string, speed: number) {
        const results: any = await SQLPool.query(
            "CALL reportLocation(?,?,?,?)",
            [lat, lon, token, speed]
        );

        return results;
    }

    static async getLocations(routeId: number) {
        const results: any = await SQLPool.query(
            "CALL getLocations(?)",
            [routeId]
        );

        return results;
    }

    static async getWaitTime(routeId: number, stopId: number) {
        const results: any = await SQLPool.query(
            "CALL getWaitTimeForStop(?,?)",
            [routeId, stopId]
        );

        return results;
    }

    static async getWaitTimeForDriver(routeId: number, stopId: number, identifier: string) {
        const results: any = await SQLPool.query(
            "CALL getWaitTimeForDriver(?,?,?)",
            [routeId, stopId, identifier]
        );

        return results;
    }
}