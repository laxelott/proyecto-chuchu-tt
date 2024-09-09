import pkg from 'express';
const { e: express } = pkg;
var router = express.Router();
import { TransportDataDAO } from '../database/transportDataDAO.js';

router.post('/transport/list/', function(req, res, next) {
    TransportDataDAO.getTransports()
        .then((result) => {res.send(JSON.stringify(result))})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/route/list/:transportId', function(req, res, next) {
    let transportId = req.params.transportId;

    TransportDataDAO.getRoutes(transportId)
        .then((result) => {res.send(JSON.stringify(result))})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post('/stop/list/:routeId', function(req, res, next) {
    let routeId = req.params.routeId;

    TransportDataDAO.getStops(routeId)
        .then((result) => {res.send(JSON.stringify(result))})
        .catch((err) => {res.send("ERROR: " + err)});
});

export default router;