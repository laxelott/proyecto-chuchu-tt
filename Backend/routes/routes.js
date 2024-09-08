var express = require('express');
var router = express.Router();
const GeneralDAO = require('../database/database');

GeneralDAO.generatePool();

/* GET home page. */
router.post('/getRouteFor', function(req, res, next) {
    let user = req.body.routeName;

    GeneralDAO.authorizeUser(user, pass)
        .then((result) => {res.send(result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

module.exports = router;
