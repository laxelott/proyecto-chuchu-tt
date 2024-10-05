import SQLPool from './conn/databaseConnection.js';

export class VehicleDataDAO {
    
    static async getVehiculeData(driverId: Number) {
        const results: any = await SQLPool.query(
            "CALL getVehicleFromDriver(?)", //Falta crear el SP de la función
            [driverId]
        );
        return results;
    }
    
    static async getVehicleLocation(driverId: Number) {
        const results: any = await SQLPool.query(
            "CALL getVehicleLocation(?)", //Falta crear el SP de la función
            [driverId]
        );
        return results;
    }
}