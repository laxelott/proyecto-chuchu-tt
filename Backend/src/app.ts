import createError from 'http-errors';
import express, { Express, Request, Response } from 'express';
import path from 'path';
import cookieParser from 'cookie-parser';
import logger from 'morgan';

import indexRouter from './routes/indexRouter.js';
import authRouter from './routes/authRouter.js';
import transportDataRouter from './routes/transportDataRouter.js';
import locationRouter from './routes/locationRouter.js';
import incidentsRouter from './incidents/incidentsDataRouter.js';

const app:Express = express();

// view engine setup
app.set('views', path.join(__dirname, './../assets/views'));
app.set('view engine', 'jade');

app.use(logger('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, './../assets/public')));


// routers
app.use('/', indexRouter);
app.use('/api/auth/', authRouter);
app.use('/api/data/', transportDataRouter);
app.use('/api/location/', locationRouter);
app.use('/api/incidents/', incidentsRouter);


// catch 404 and forward to error handler
app.use(function(req, res, next) {
    next(createError(404));
});

// error handler
app.use(function(err: any, req: Request, res: Response) {
    // set locals, only providing error in development
    res.locals.message = err.message;
    res.locals.error = req.app.get('env') === 'development' ? err : {};

    // render the error page
    res.status(err.status || 500);
    res.render('error');
});

export default app;