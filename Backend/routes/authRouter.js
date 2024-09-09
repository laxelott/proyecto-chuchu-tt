import pkg from 'express';
const { e: express } = pkg;
var router = express.Router();
import { AuthDAO } from '../database/authDAO.js';

/* GET home page. */
router.post('/authorize', function(req, res, next) {
    let username = req.body.username;
    let password = req.body.password;

    AuthDAO.authorizeUser(username, password)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

export default router;