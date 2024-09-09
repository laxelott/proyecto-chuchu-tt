import { createPool } from "promise-mysql";

export const dbPool = createPool({
        host: `${process.env.DB_HOST}`,
        user: process.env.DB_USER,
        password: process.env.DB_PASS,
        password: process.env.DB_PASS,
        database: process.env.DB_NAME,
        socketPath: `${process.env.DB_HOST}`,
        waitForConnections: true,
        connectionLimit: 10,
        queueLimit: 0
});