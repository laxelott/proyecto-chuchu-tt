import { RowDataPacket } from "mysql2";
import { createPool, Pool, QueryResult } from "mysql2/promise";
import { ppid } from "process";

const options = {
    host: `${process.env.DB_HOST}`,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    socketPath: `${process.env.DB_HOST}`,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

class SQLPool {
    dbPool: Pool;

    constructor() {
        this.dbPool = createPool(options);
    }

    async query(query: string, args: any[] = []) {
        const results: any = (await this.dbPool.execute( query, args ))[0];
        return results[0];
    }
}



export default new SQLPool();