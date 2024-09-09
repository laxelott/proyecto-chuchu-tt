import { Router } from 'express';
import { AuthDAO } from '../database/authDAO.js';

const router  = Router();

router.get('/', function(req, res, next) {
    AuthDAO.testData()
        .then((result) => {res.send("APP CHUCHU " + result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

export default router;