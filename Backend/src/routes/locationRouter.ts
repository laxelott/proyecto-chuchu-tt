import { Router } from 'express';
import { LocationDAO } from '../database/locationDAO.js';

const router  = Router();

router.post('/register/:lat/:long', function(req, res, next) {
    let lat = Number.parseFloat(req.params.lat);
    let long = Number.parseFloat(req.params.long);

    LocationDAO.writeLocation(lat, long);
})

router.post('/reportLocation/:lat/:lon', function(req, res, next) {
    let lat = Number.parseFloat(req.params.lat);
    let lon = Number.parseFloat(req.params.lon);
    let token: string = req.body.token;
    let speed: number = req.body.speed;

    LocationDAO.reportLocation(lat, lon, token, speed)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/getLocations/:routeId', function(req, res, next) {
    let routeId = Number.parseFloat(req.params.routeId);

    LocationDAO.getLocations(routeId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/getWaitTime/:routeId/:stopId', function(req, res, next) {
    let routeId = Number.parseFloat(req.params.routeId);
    let stopId = Number.parseFloat(req.params.stopId);

    LocationDAO.getWaitTime(routeId, stopId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/getWaitTimeForVehicle/:routeId/:stopId/:identifier', function(req, res, next) {
    let routeId = Number.parseFloat(req.params.routeId);
    let stopId = Number.parseFloat(req.params.stopId);
    let identifier = req.params.identifier;

    LocationDAO.getWaitTimeForVehicle(routeId, stopId, identifier)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

export default router;