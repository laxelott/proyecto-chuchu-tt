import SQLPool from './conn/databaseConnection.js';

export class AdminDAO {
    static async resetSystem() {
        await SQLPool.query("CALL resetData()");
        return {error: 0}
    }
    // Transporte
    static async getTransportsAdmin() {
        const results = await SQLPool.query("CALL getTransportsAdmin()");
        return results;
    }

    static async addTransportAdmin(name: string) {
        const results = await SQLPool.query("CALL upsertTransport(?)", [name]);
        return results;
    }


    // Rutas
    static async getRoutesAdmin() {
        const results = await SQLPool.query("CALL getRoutesAdmin()");
        return results;
    }

    static async addRouteAdmin(idruta: number, idtransport: number, nombreruta: string, description: string, color: string) {
        const results = await SQLPool.query("CALL upsertRoute(?, ?, ?, ?, ?)", [idruta, idtransport, nombreruta, description, color]);
        return results;
    }

    // Añadir las mismas funciones para las demás tablas (vehículos, conductores y paradas)

    // Vehículos
    static async getVehiclesAdmin() {
        const results = await SQLPool.query("CALL getVehiclesAdmin()");
        return results;
    }

    static async addVehicle(identificador: string, idconductor: number, curp: string) {
        const results = await SQLPool.query("CALL upsertVehicle(?, ?, ?)", [identificador, idconductor, curp]);
        return results;
    }


    // Conductores
    static async getDriversAdmin() {
        const results = await SQLPool.query("CALL getDriversAdmin()");
        return results;
    }

    static async upsertDriver(curp: string, name: string, surnamep: string, surnamem: string, password: string, salt: string, phone: string, active: number) {
        const results = await SQLPool.query("CALL upsertDriver(?, ?, ?, ?, ?, ?, ?, ?)", [curp, name, surnamep, surnamem, password, salt, phone, active]);
        return results;
    }


    // Estaciones
    static async getStopsAdmin() {
        const results = await SQLPool.query("CALL getStopsAdmin()");
        return results;
    }

    static async addStops(idruta: number, name: string, latitude: number, longitude: number, nombrenext: string) {
        const results = await SQLPool.query("CALL upsertStop(?, ?, ?, ?, ?)", [idruta, name, latitude, longitude, nombrenext]);
        return results;
    }

}
