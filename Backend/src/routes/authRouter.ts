import { Router } from 'express';
import { AuthDAO } from '../database/authDAO.js';

const router  = Router();

router.post('/login', function(req, res, next) {
    let username = req.body.username;
    let password = req.body.password;

    AuthDAO.loginDriver(username, password)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post('/logout', function(req, res, next) {
    let token = req.body.token;
    
    AuthDAO.logoutDriver(token)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

router.post("/getHashedKey/:username/:password", (req, res) => {
    let username = req.params.username
    let password = req.params.password

    AuthDAO.getHashedPassword(username, password)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
})

export default router;