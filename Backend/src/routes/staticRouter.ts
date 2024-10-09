import { Router } from 'express';
import { AuthDAO } from '../database/authDAO.js';

const router  = Router();

router.get('/', function(req, res, next) {
    res.render("index", {})
});
router.get('/pasajero', function(req, res, next) {
    res.render("pasajero", {})
});
router.get('/conductor', function(req, res, next) {
    res.render("conductor", {})
});
router.get('/error', function(req, res, next) {
    res.render("error", {})
});
router.get('/404', function(req, res, next) {
    res.render("404", {})
});
router.get('/layout', function(req, res, next) {
    res.render("layout", {})
});
router.get('/login', function(req, res, next) {
    res.render("login", {})
});

export default router;