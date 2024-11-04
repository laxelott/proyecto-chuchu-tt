import SQLPool from './conn/databaseConnection.js';

export class IncidentsDAO {
    static async getIncidents(routeId: Number) {
        const results: any = await SQLPool.query(
            "CALL getIncidents(?)",
            [routeId]
        );

        return results;
    }

    static async addIncident(incidentType: Number, routeId: Number, lon: any, lat: any, token: string, description: string) {

        const results: any = await SQLPool.query(
            "CALL addIncident(?,?,?,?,?,?)",
            [incidentType, routeId, lon, lat, token, description]
        );

        return results;
    }

    static async removeIncident(incidentId: Number, token: string) {
        const results: any = await SQLPool.query(
            "CALL removeIncident(?,?)",
            [incidentId, token]
        );

        return results;
    }
}