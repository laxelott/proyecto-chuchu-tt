import { RowDataPacket } from 'mysql2';
import { dbPool } from './conn/databaseConnection.js';

export class TransportDataDAO {
    static async getTransports() {
        const result: any = (await dbPool).query<RowDataPacket[]>(
            "CALL getTransports()"
        );
        return result[0];
    }
    
    static async getRoutes(transportId: Number) {
        const result: any = (await dbPool).query<RowDataPacket[]>(
            "CALL getRoutesFromTransport(?)",
            [transportId]
        );
        return result[0];
    }
    
    static async getStops(routeId: Number) {
        const result: any = (await dbPool).query<RowDataPacket[]>(
            "CALL getStopsFromRoute(?)",
            [routeId]
        );
        return result[0];
    }
}