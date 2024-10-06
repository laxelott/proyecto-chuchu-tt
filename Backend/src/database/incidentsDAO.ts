import SQLPool from './conn/databaseConnection.js';

export class incidentsDAO {
    static async getIncidents(routeId: Number) {
        const results: any = await SQLPool.query(
            "CALL getIncidents(?)",
            [routeId]
        );

        return results;
    }
}