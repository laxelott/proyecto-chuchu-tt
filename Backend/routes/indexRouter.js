import pkg from 'express';
const { e: express } = pkg;
var router = express.Router();
import { AuthDAO } from '../database/authDAO.js';

/* GET home page. */
router.get('/', function(req, res, next) {
    AuthDAO.testDB()
        .then((result) => {res.send("APP CHUCHU " + result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

export default router;