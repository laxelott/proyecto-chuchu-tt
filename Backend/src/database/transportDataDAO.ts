import SQLPool from './conn/databaseConnection.js';

export class TransportDataDAO {
    static async getTransports() {
        const results: any = await SQLPool.query(
            "CALL getTransports()"
        );

        return results;
    }
    
    static async getRoutes(transportId: Number) {
        const results: any = await SQLPool.query(
            "CALL getRoutesFromTransport(?)",
            [transportId]
        );
        return results;
    }
    
    static async getStops(routeId: Number) {
        const results: any = await SQLPool.query(
            "CALL getStopsFromRoute(?)",
            [routeId]
        );
        return results;
    }
}