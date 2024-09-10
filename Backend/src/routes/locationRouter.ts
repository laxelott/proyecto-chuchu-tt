import { Router } from 'express';
import { LocationDAO } from '../database/locationDAO.js';

const router  = Router();

router.post('/register/:lat/:long', function(req, res, next) {
    let lat = Number.parseFloat(req.params.lat);
    let long = Number.parseFloat(req.params.long);

    console.log(`write location: (${lat}, ${long})`)

    LocationDAO.writeLocation(lat, long);
})

export default router;