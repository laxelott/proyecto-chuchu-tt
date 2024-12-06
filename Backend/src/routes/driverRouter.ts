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

    DriverDAO.useVehicle(vehicleIdentifier, token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post('/leaveVehicle', function(req, res, next) {
    let token = req.body.token;

    console.log("leaveVehicle")

    DriverDAO.leaveVehicle(token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/startTrip/:routeId', function(req, res, next) {
    let token = req.body.token;
    let routeId = req.params.routeId;

    console.log("----- Start Trip, " + token + ", " + routeId)
    
    DriverDAO.startTrip(token, routeId)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/endTrip/', function(req, res, next) {
    let token = req.body.token;

    console.log("----- End Trip, " + token)
    
    DriverDAO.endTrip(token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/cancelTrip', function(req, res, next) {
    let token = req.body.token;
    let reason = req.body.reason;

    DriverDAO.cancelTrip(token, reason)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/getWaitTime', function(req, res, next) {
    let token = req.body.token;

    DriverDAO.getWaitTime(token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

export default router;