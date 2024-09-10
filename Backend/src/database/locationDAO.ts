import SQLPool from './conn/databaseConnection.js';

export class LocationDAO {
    static async writeLocation(lat: number, long: number) {
        const results: any = await SQLPool.query(
            "INSERT INTO Waypoint(coordX, coordY) VALUES (?, ?)",
            [lat, long]
        );

        return results;
    }
}