import SQLPool from './conn/databaseConnection.js';

export class LocationDAO {
    static async writeLocation(lat: number, lon: number) {
        const results: any = await SQLPool.query(
            "INSERT INTO Waypoint(coordX, coordY) VALUES (?, ?)",
            [lat, lon]
        );

        return results;
    }

    static async reportLocation(lat: number, lon: number, token: string) {
        const results: any = await SQLPool.query(
            "CALL reportLocation(?,?,?)",
            [lat, lon, token]
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
}