import { dbPool } from './databaseConnection.js';

export class TransportDataDAO {
    async getTransports() {
        const result  = (await dbPool).query(
            "CALL getTransports()"
        );
        return result[0];
    }
    
    async getRoutes(transportId) {
        const result  = (await dbPool).query(
            "CALL getRoutesFromTransport(?)",
            [transportId]
        );
        return result[0];
    }
    
    async getStops(routeId) {
        const result  = (await dbPool).query(
            "CALL getStopsFromRoute(?)",
            [routeId]
        );
        return result[0];
    }
}