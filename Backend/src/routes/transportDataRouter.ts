import { Router } from 'express';
import { TransportDataDAO } from '../database/transportDataDAO.js';

const router  = Router();

router.post('/transport/list/', function(req, res, next) {
    TransportDataDAO.getTransports()
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/route/list/:transportId', function(req, res, next) {
    let transportId:Number = Number.parseInt(req.params.transportId);

    TransportDataDAO.getRoutes(transportId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post('/stop/list/:routeId', function(req, res, next) {
    let routeId:Number = Number.parseInt(req.params.routeId);

    TransportDataDAO.getStops(routeId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

export default router;