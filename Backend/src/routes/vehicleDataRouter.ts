import { Router } from 'express';
import { VehicleDataDAO } from '../database/vehicleDataDAO.js';

const router  = Router();

/*router.post('/transport/list/', function(req, res, next) {
    let driverId:Number = Number.parseInt(req.params.driverId);
    
    VehicleDataDAO.getVehiculeData(driverId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

router.post('/route/list/:transportId', function(req, res, next) {
    let driverId:Number = Number.parseInt(req.params.driverId);

    VehicleDataDAO.getVehicleLocation(driverId)
        .then((result) => {res.json(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});*/


export default router;