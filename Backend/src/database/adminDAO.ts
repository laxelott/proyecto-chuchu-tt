import SQLPool from './conn/databaseConnection.js';

export class AdminDAO {
    // Transporte
    static async getTransportsAdmin() {
        const results = await SQLPool.query("CALL getTransportsAdmin()");
        return results;
    }

    static async addTransportAdmin(name: string, iconB64: string) {
        const results = await SQLPool.query("CALL addTransportAdmin(?, ?)", [name, iconB64]);
        return results;
    }

    static async updateTransportAdmin(idTransport: number, name: string, iconB64: string) {
        const results = await SQLPool.query("CALL updateTransportAdmin(?, ?, ?)", [idTransport, name, iconB64]);
        return results;
    }

    // Rutas
    static async getRoutesAdmin() {
        const results = await SQLPool.query("CALL getRoutesAdmin()");
        return results;
    }

    static async addRouteAdmin(name: string, description: string, color: string, iconB64: string, idTransport: number) {
        const results = await SQLPool.query("CALL addRouteAdmin(?, ?, ?, ?, ?)", [name, description, color, iconB64, idTransport]);
        return results;
    }

    static async updateRouteAdmin(idRoute: number, name: string, description: string, color: string, iconB64: string) {
        const results = await SQLPool.query("CALL updateRouteAdmin(?, ?, ?, ?, ?)", [idRoute, name, description, color, iconB64]);
        return results;
    }

    // Añadir las mismas funciones para las demás tablas (vehículos, conductores y paradas)

    // Vehículos
    static async getVehiclesAdmin() {
        const results = await SQLPool.query("CALL getVehiclesAdmin()");
        return results;
    }

    static async addVehicle(name: string, description: string, color: string, iconB64: string, idTransport: number) {
        const results = await SQLPool.query("CALL addVehicle(?, ?, ?, ?, ?)", [name, description, color, iconB64, idTransport]);
        return results;
    }

    static async updateVehicle(idRoute: number, name: string, description: string, color: string, iconB64: string) {
        const results = await SQLPool.query("CALL updateVehicle(?, ?, ?, ?, ?)", [idRoute, name, description, color, iconB64]);
        return results;
    }

    // Conductores
    static async getDriversAdmin() {
        const results = await SQLPool.query("CALL getDriversAdmin()");
        return results;
    }

    static async addDriver(name: string, description: string, color: string, iconB64: string, idTransport: number) {
        const results = await SQLPool.query("CALL addDriverAdmin(?, ?, ?, ?, ?)", [name, description, color, iconB64, idTransport]);
        return results;
    }

    static async updateDriver(idRoute: number, name: string, description: string, color: string, iconB64: string) {
        const results = await SQLPool.query("CALL updateDriverAdmin(?, ?, ?, ?, ?)", [idRoute, name, description, color, iconB64]);
        return results;
    }

    // Estaciones
    static async getStopsAdmin() {
        const results = await SQLPool.query("CALL getStopsAdmin()");
        return results;
    }

    static async addStops(name: string, description: string, color: string, iconB64: string, idTransport: number) {
        const results = await SQLPool.query("CALL addStopsAdmin(?, ?, ?, ?, ?)", [name, description, color, iconB64, idTransport]);
        return results;
    }

    static async updateStops(idRoute: number, name: string, description: string, color: string, iconB64: string) {
        const results = await SQLPool.query("CALL updateStopsAdmin(?, ?, ?, ?, ?)", [idRoute, name, description, color, iconB64]);
        return results;
    }
}
