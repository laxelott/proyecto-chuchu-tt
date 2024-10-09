import { Router } from 'express';
import { DriverDAO } from '../database/driverDAO.js';

const router = Router();

router.post('/getInfo', function(req, res, next) {
    let token = req.body.token;

    DriverDAO.getDriverInfo(token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

export default router;