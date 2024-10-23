import { Router } from 'express';
import { IncidentsDAO } from '../database/incidentsDAO.js';

const router  = Router();

router.post('/list/:routeId', function(req, res, next) {
    let routeId:Number = Number.parseInt(req.params.routeId);

    IncidentsDAO.getIncidents(routeId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/add/:incidentType/:routeId', function(req, res, next) {
    let incidentType:Number = Number.parseInt(req.params.incidentType);
    let rotueId:Number = Number.parseInt(req.params.routeId);
    let description: string = req.body.description;
    let token: string = req.body.token;
    let lon = req.body.lon;
    let lat = req.body.lat;


    IncidentsDAO.addIncident(incidentType, rotueId, lon, lat, token, description)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/remove/:incidentId', function(req, res, next) {
    let incidentId:Number = Number.parseInt(req.params.incidentId);
    let token: string = req.body.token;

    IncidentsDAO.removeIncident(incidentId, token)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

export default router;