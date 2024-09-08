var express = require('express');
var router = express.Router();
const GeneralDAO = require('../database/database');

GeneralDAO.generatePool();

/* GET home page. */
router.post('/authorize', function(req, res, next) {
    let user = req.body.user;
    let pass = req.body.pass;

    GeneralDAO.authorizeUser(user, pass)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

module.exports = router;
