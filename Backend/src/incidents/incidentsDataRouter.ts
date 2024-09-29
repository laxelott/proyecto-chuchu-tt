import { Router } from 'express';
import { incidentsDAO } from '../database/incidentsDAO.js';

const router  = Router();

router.post('/incident/list/:routeId', function(req, res, next) {
    let routeId:Number = Number.parseInt(req.params.routeId);

    incidentsDAO.getIncidents(routeId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

export default router;