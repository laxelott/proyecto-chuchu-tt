import { Router } from 'express';
import { DriverDAO } from '../database/driverDAO.js';

const router = Router();

router.post('/getInfo', function(req, res, next) {
    let token = req.body.token;

    DriverDAO.getDriverInfo(token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post('/arrivedToStop/:idStop', function(req, res, next) {
    let token = req.body.token;
    let idStop = req.params.idStop;

    DriverDAO.arrivedToStop(token, idStop)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post('/useVehicle/:vehicleIdentifier', function(req, res, next) {
    let vehicleIdentifier = req.params.vehicleIdentifier;
    let token = req.body.token;

    console.log("useVehicle")
    console.log(vehicleIdentifier)
    console.log(token)

    DriverDAO.useVehicle(vehicleIdentifier, token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post('/leaveVehicle', function(req, res, next) {
    let token = req.body.token;

    console.log("useVehicle")
    console.log(token)

    DriverDAO.leaveVehicle(token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

export default router;