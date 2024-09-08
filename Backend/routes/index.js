var express = require('express');
var router = express.Router();
const GeneralDAO = require('../database/database');

/* GET home page. */
router.get('/', function(req, res, next) {
    GeneralDAO.testDB()
        .then((result) => {res.send("APP CHUCHU " + result)})
        .catch((err) => {res.send("ERROR: " + err)});
});

module.exports = router;
