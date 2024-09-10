import { createPool, Pool, PoolOptions, QueryResult } from "mysql2/promise";

let options: PoolOptions = {
    host: `${process.env.DB_HOST}`,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

if (process.env.NODE_ENV != 'dev') {
    options.socketPath = `${process.env.DB_HOST}`;
}

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