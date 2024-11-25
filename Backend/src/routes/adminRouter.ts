import { Router } from 'express';
import { AdminDAO } from '../database/adminDAO.js';

const router = Router();

// Transporte
router.get('/transports', (req, res) => {
    AdminDAO.getTransportsAdmin()
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

router.post('/transports', (req, res) => {
    const { name } = req.body;
    AdminDAO.addTransportAdmin(name)
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

// Rutas
router.get('/routes', (req, res) => {
    AdminDAO.getRoutesAdmin()
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

router.post('/routes', (req, res) => {
    const { idruta, idtransport, nombreruta, description, color } = req.body;
    AdminDAO.addRouteAdmin(idruta, idtransport, nombreruta, description, color)
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

// Vehículos
router.get('/vehicles', (req, res) => {
    AdminDAO.getVehiclesAdmin()
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

router.post('/vehicles', (req, res) => {
    const { identificador, idconductor, curp } = req.body;
    AdminDAO.addVehicle(identificador, idconductor, curp)
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

// Conductores
router.get('/drivers', (req, res) => {
    AdminDAO.getDriversAdmin()
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

router.post('/drivers', (req, res) => {
    const { curp, name, surnamep, surnamem, password, salt, phone, active } = req.body;
    AdminDAO.upsertDriver(curp, name, surnamep, surnamem, password, salt, phone, active)
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

// Estaciones
router.get('/stops', (req, res) => {
    AdminDAO.getStopsAdmin()
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

router.post('/stops', (req, res) => {
    const { idruta, name, latitude, longitude, nombrenext } = req.body;
    AdminDAO.addStops(idruta, name, latitude, longitude, nombrenext)
        .then((result) => res.json(result))
        .catch((err) => res.status(500).send("ERROR: " + err));
});

export default router;
