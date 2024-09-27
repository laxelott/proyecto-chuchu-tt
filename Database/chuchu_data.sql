USE Chuchu;

SET foreign_key_checks = 0;

INSERT INTO Admin(idAdmin, username, password, salt) VALUES
    (1101, N'migue', N'123chu#', N'4731f0c7c3fb2e7c7164307eb71d0372');

INSERT INTO `Admin_Transport` VALUES (3001, 2001, 1101);

CALL upsertDriver(N'MAVJ900826HSRRLN36', N'JUAN', N'MARTINEZ', N'VALDEZ', N'a19394ba230a67b2a0d26bb85667810589797d22fed10c89710aeae0bbec3e441d31d0229bfabde97a35bff6804a74224124', N'5fa35f24bf31f7100bc81dfad76ea8a1', N'5548526987', 1);
CALL upsertDriver(N'HEME900926HPLRTN67', N'ENRIQUE', N'HERNANDEZ', N'MITOTE', N'1076dbd745705eeee12299f72201390ff7d277ccfa960bd19a87f746640b209e2a26b891f747a0de3a8ad5d04b7af34b3ee9', N'53a83e4d065fc71ddabaece70d6d5b9a', N'5589684751', 1);
CALL upsertDriver(N'LUCR900926HGRNBF19', N'RAFAEL', N'LUNA', N'CABALLERO', N'b96c682999ca5b1735bc628d96b7e4f2f884f60e15a711b6fe5803802ea5997c1653834f7040de98f88a8f843a712c9cae39', N'a3518c4a5ad17a9134fd303dab5e621f', N'5589898989', 1);
CALL upsertDriver(N'DOSM900928HCXMBT25', N'MATEO', N'DOMINGUEZ', N'SABADO', N'dfd3b2e9a350ba7d247cfd51d436bf000271dfa71bd57112b1663042417bbc8c4aff136900496194a9357b20281d34e0b629', N'3679a607c5dee7fa5cad98502ac2c38f', N'5514584122', 1);
CALL upsertDriver(N'MECJ901215HCXNRS17', N'JOSE', N'MENDOZA', N'CRUZ', N'08e204d71aa921e5921b8243790b2fce9b95b58ea73a797f21781cb6dcf69ebedd0deddaa164249c4507df0a08d7f49819de', N'a9ce145e76a51772a21ba03ac04f0ec4', N'5568989856', 1);
CALL upsertDriver(N'MERJ901215HCXNYR31', N'JORGE', N'MENDEZ', N'REYEZ', N'1d86cdfb45bf2bfc4e6e7e2f59f9a1578ecb0420c8ecab154d08ca3aef9a5f4e1c37ecd3d67d641e635718bb4a8b283f48ce', N'2021c195b8cb06adce9d3cc1adc86210', N'5587848956', 1);
CALL upsertDriver(N'JIGV900415MCXMNL23', N'VALENTINA', N'JIMENEZ', N'GONZALES', N'e6cae96e72de2562f5a7d3971f81c36414d3820131fcef6222ec82569a315c3f6b55c4f7bc8fea83e1067ac69598cb864f9a', N'307e7e8ef28d15a1a1623e6d9c81d924', N'5512121212', 1);
CALL upsertDriver(N'VAMG900815MCXRRB94', N'GABRIELA', N'VARGAS', N'MORENO', N'ed607f39bbe8762ee932238bc46098f372621e3ffb9b624952976595ecb1f31351608c9939e168f206d518533ddcbf725651', N'3378c81f6150557bd2514b1e546999a1', N'5565986569', 1);
CALL upsertDriver(N'MOVM900815MCXRRR51', N'MARIA', N'MORENO', N'VARGAS', N'4e890403749629ab109da4275f791e47ae716455d20ca7314c31035e9c7c51218fd31212014ecca8fbe68d5163babfaf670e', N'9d508fdb4d4b4745cd0d5b6393d56b05', N'5569847115', 1);
CALL upsertDriver(N'JAHN9601222MCXNMN3', N'ANDREA', N'GONZALES', N'JIMENEZ', N'ecf035adc44ba35b119694201d7c8fdc3d36500f94f32169c180590c1f9a661a4f55b8da55cef4955adb2aabbcfd4be5c664', N'6cb05d0ba29e79a66c2902da326885fa', N'5525222020', 1);

INSERT INTO `Vehicle`(idVehicle, identifier) VALUES (40001, N'A01-AGA');
INSERT INTO `Vehicle`(idVehicle, identifier) VALUES (40002, N'A74-CMA');
INSERT INTO `Vehicle`(idVehicle, identifier) VALUES (40003, N'A25-ARA');
INSERT INTO `Vehicle`(idVehicle, identifier) VALUES (40004, N'A78-ERE');
INSERT INTO `Vehicle`(idVehicle, identifier) VALUES (40005, N'A96-HUE');

CALL linkDriverVehicle('MAVJ900826HSRRLN36', 'A01-AGA');
CALL linkDriverVehicle('MAVJ900826HSRRLN36', 'A74-CMA');
CALL linkDriverVehicle('JIGV900415MCXMNL23', 'A74-CMA');
CALL linkDriverVehicle('LUCR900926HGRNBF19', 'A25-ARA');
CALL linkDriverVehicle('MECJ901215HCXNRS17', 'A78-ERE');
CALL linkDriverVehicle('VAMG900815MCXRRB94', 'A78-ERE');
CALL linkDriverVehicle('JAHN9601222MCXNMN3', 'A78-ERE');


INSERT INTO `Transport`(idTransport, name, iconB64) VALUES (2001, N'Trolebus','PHZlY3RvciB4bWxuczphbmRyb2lkPSJodHRwOi8vc2NoZW1hcy5hbmRyb2lkLmNvbS9hcGsvcmVzL2FuZHJvaWQiCiAgICBhbmRyb2lkOndpZHRoPSI1MGRwIgogICAgYW5kcm9pZDpoZWlnaHQ9IjUwZHAiCiAgICBhbmRyb2lkOnZpZXdwb3J0V2lkdGg9Ijk3NSIKICAgIGFuZHJvaWQ6dmlld3BvcnRIZWlnaHQ9Ijk3NSI+CiAgPGdyb3VwPgogICAgPGNsaXAtcGF0aAogICAgICAgIGFuZHJvaWQ6cGF0aERhdGE9Ik0wLDBoOTc1djk3NWgtOTc1eiIvPgogICAgPHBhdGgKICAgICAgICBhbmRyb2lkOnBhdGhEYXRhPSJNOTczLjM0LDY2LjU2Qzk3My4zNCwzMC4yNCA5NDMuOTcsMC44IDkwNy43NiwwLjhMNTM5Ljk0LDAuOCA0ODcuMTQsNTMuODQgNDM0LjMzLDAuOCA2NS43MSwwLjhDMjkuNSwwLjggMC4xNCwzMC4yNCAwLjE0LDY2LjU2TDAuMTQsNDM1LjQgNTIuMyw0ODcuOCAwLjE0LDU0MC4yIDAuMTQsOTA5LjA0QzAuMTQsOTQ1LjM2IDI5LjUsOTc0LjggNjUuNzEsOTc0LjhMNDM2Ljk1LDk3NC44IDQ4Ny4xNCw5MjQuMzkgNTM3LjMyLDk3NC44IDkwOC41Niw5NzQuOEM5NDQuNzcsOTc0LjggOTc0LjE0LDk0NS4zNiA5NzQuMTQsOTA5LjA0TDk3NC4xNCw1NDAuOTIgOTIxLjI2LDQ4Ny44IDk3NC4xNCw0MzQuNjlaIgogICAgICAgIGFuZHJvaWQ6ZmlsbENvbG9yPSIjMDA1N0I4IgogICAgICAgIGFuZHJvaWQ6ZmlsbFR5cGU9ImV2ZW5PZGQiLz4KICAgIDxwYXRoCiAgICAgICAgYW5kcm9pZDpwYXRoRGF0YT0iTTY1My45OCw3NjUuODQgTDMyNS4xOSw3NjUuODQgNTA0LjkyLDQ1NC43MiA0MzAuMTYsNDU0LjcyIDI1MC40MSw3NjUuODQgMTQ2LjEyLDc2NS44NCAzMjUuODIsNDU0LjcyIDE3OC4zNiw0NTQuNzIgMTc4LjM2LDM2NC40IDM3Ny45OCwzNjQuNCA0MTUuNjgsMjk5LjE2IDE3OC4zNiwyOTkuMTYgMTc4LjM2LDIwOC44NCA4MjcuMTIsMjA4Ljg0IDgyNy4xMiwyOTkuMTYgNTE5Ljk4LDI5OS4xNiA0ODIuMywzNjQuNCA4MjcuMTIsMzY0LjQgODI3LjEyLDQ1NC43MiA2MDkuMjQsNDU0LjcyIDQ4MS43Miw2NzUuNTUgNjUzLjk4LDY3NS41NVoiCiAgICAgICAgYW5kcm9pZDpmaWxsQ29sb3I9IiNGRkZGRkYiCiAgICAgICAgYW5kcm9pZDpmaWxsVHlwZT0iZXZlbk9kZCIvPgogICAgPHBhdGgKICAgICAgICBhbmRyb2lkOnBhdGhEYXRhPSJNNzQwLjEzLDYxMC44MiBMNTkzLjEzLDYxMC44MiA2NDUuMDUsNTE5LjgyIDc0MC4xMyw1MTkuODJaIgogICAgICAgIGFuZHJvaWQ6ZmlsbENvbG9yPSIjRkZGRkZGIgogICAgICAgIGFuZHJvaWQ6ZmlsbFR5cGU9ImV2ZW5PZGQiLz4KICA8L2dyb3VwPgo8L3ZlY3Rvcj4K');
INSERT INTO `Route`(idRoute, idTransport, name, description, color) VALUES (60001, 2001, N'Linea 8', N'Circuito Politécnico', N'FF2B78E4');

CALL upsertStop(60001, N'El Queso', 19.4949401551899, -99.1349875800365, NULL);
CALL upsertStop(60001, N'Oroya', 19.4949770293232, -99.1363467471817, N'El Queso');
CALL upsertStop(60001, N'Huacho', 19.495667121671, -99.1395313924074, N'Oroya');
CALL upsertStop(60001, N'Av. 45 Metros', 19.4961453076498, -99.1407199775522, N'Huacho');
CALL upsertStop(60001, N'Fte. Centro de Formación Educativa', 19.4967571855356, -99.1421566028128, N'Av. 45 Metros');
CALL upsertStop(60001, N'Via Ceti', 19.4976117865935, -99.1439697760968, N'Fte. Centro de Formación Educativa');
CALL upsertStop(60001, N'Salvatierra', 19.4984335141151, -99.1459143775843, N'Via Ceti');
CALL upsertStop(60001, N'Cda. Manuel Stampa', 19.4991367227318, -99.1467941810934, N'Salvatierra');
CALL upsertStop(60001, N'Esc. Nacional de Ciencias Biológicas', 19.498705182378, -99.1455784559949, N'Cda. Manuel Stampa');
CALL upsertStop(60001, N'Centro de Formación Educativa', 19.497007885578, -99.1418908960169, N'Esc. Nacional de Ciencias Biológicas');
CALL upsertStop(60001, N'Planetario', 19.4962231086011, -99.14021630252, N'Centro de Formación Educativa');
CALL upsertStop(60001, N'Cenlex', 19.4959885768089, -99.1392849095302, N'Planetario');
CALL upsertStop(60001, N'Fte. Edif. 1 Esime', 19.4978560799053, -99.136714934243, N'Cenlex');
CALL upsertStop(60001, N'Fte. Edif. 2 Esime', 19.4984609699412, -99.1365764743687, N'Fte. Edif. 1 Esime');
CALL upsertStop(60001, N'Fte. Edif. 4 Esime', 19.499880164643, -99.1362567513563, N'Fte. Edif. 2 Esime');
CALL upsertStop(60001, N'Fte. Edif. 6 Esiqie', 19.5011746723893, -99.1360689967739, N'Fte. Edif. 4 Esime');
CALL upsertStop(60001, N'Fte. Edif. 8 Esiqie', 19.5024388299327, -99.1357685893982, N'Fte. Edif. 6 Esiqie');
CALL upsertStop(60001, N'Manuel de Anda Y Barredo', 19.503582706565, -99.1359698702366, N'Fte. Edif. 8 Esiqie');
CALL upsertStop(60001, N'Fte. Edif 11 Esia', 19.503738841312, -99.1371175635296, N'Manuel de Anda Y Barredo');
CALL upsertStop(60001, N'Cancha de Entrenamiento Pieles Rojas', 19.5042114535381, -99.1394787715645, N'Fte. Edif 11 Esia');
CALL upsertStop(60001, N'Fte. Central de Inteligencia de Cómputo', 19.5046859557844, -99.1422316747015, N'Cancha de Entrenamiento Pieles Rojas');
CALL upsertStop(60001, N'R Ma. Luisa Stampa Orti;za', 19.5048635499717, -99.1432228422527, N'Fte. Central de Inteligencia de Cómputo');
CALL upsertStop(60001, N'Escom', 19.505844504147, -99.1455457060247, N'R Ma. Luisa Stampa Orti;za');
CALL upsertStop(60001, N'Av. Miguel Othón de Mendizábal Ote. - Av. Juan de Dios Bátiz', 19.5062512509023, -99.1472991018159, N'Escom');
CALL upsertStop(60001, N'Av. Miguel Othón de Mendizábal Ote. - Neptuno', 19.5056587067313, -99.1488440389853, N'Av. Miguel Othón de Mendizábal Ote. - Av. Juan de Dios Bátiz');
CALL upsertStop(60001, N'Eje Central Lázaro Cárdenas - Av. Miguel O. de Mendizábal Ote.', 19.5047177404643, -99.1502901918332, N'Av. Miguel Othón de Mendizábal Ote. - Neptuno');
CALL upsertStop(60001, N'Eje Central Lázaro Cárdenas - Politécnico Oriente', 19.5002001691551, -99.148505917351, N'Eje Central Lázaro Cárdenas - Av. Miguel O. de Mendizábal Ote.');
CALL upsertStop(60001, N'Eje Central Lázaro Cárdenas - Av. Wilfrido Massieu', 19.4990109740478, -99.1480355133274, N'Eje Central Lázaro Cárdenas - Politécnico Oriente');
CALL upsertStop(60001, N'Eje Central Lázado Cárdenas - Cda. de Otavalo', 19.4953208100633, -99.1465665413194, N'Eje Central Lázaro Cárdenas - Av. Wilfrido Massieu');
CALL upsertStop(60001, N'Montevideo', 19.4936406476243, -99.1466143724623, N'Eje Central Lázado Cárdenas - Cda. de Otavalo');
CALL upsertStop(60001, N'Politécnico Poniente', 19.5005476092996, -99.1496036090908, N'Montevideo');
CALL upsertStop(60001, N'Eje Central Lázaro Cárdenas - Av. Miguel. O. de Mendizábal Ote.', 19.5045199499282, -99.1511630776174, N'Politécnico Poniente');
CALL upsertStop(60001, N'Juan de D. Batiz', 19.5064720056217, -99.1464826304588, N'Eje Central Lázaro Cárdenas - Av. Miguel. O. de Mendizábal Ote.');
CALL upsertStop(60001, N'Fte. Escom', 19.5055628404822, -99.1450912693381, N'Juan de D. Batiz');
CALL upsertStop(60001, N'Ma. Luisa Stampa Orti;za', 19.5051401619989, -99.1431909452342, N'Fte. Escom');
CALL upsertStop(60001, N'Central de Inteligencia de Cómputo', 19.5049447296332, -99.1421383435297, N'Ma. Luisa Stampa Orti;za');
CALL upsertStop(60001, N'Secretaria de Extensión Y Difusión', 19.5045688974968, -99.1397460670399, N'Central de Inteligencia de Cómputo');
CALL upsertStop(60001, N'Biblioteca Esia', 19.50428927782, -99.1384159612606, N'Secretaria de Extensión Y Difusión');
CALL upsertStop(60001, N'Edif 11 Esia', 19.5041297087128, -99.1369261814186, N'Biblioteca Esia');
CALL upsertStop(60001, N'Edif. 10 Esia', 19.5039830239339, -99.1357729056504, N'Edif 11 Esia');
CALL upsertStop(60001, N'Edif. 8 Esiqie', 19.5024388299327, -99.1355593771078, N'Edif. 10 Esia');
CALL upsertStop(60001, N'Edif. 6 Esiqie', 19.5010988220785, -99.135795411471, N'Edif. 8 Esiqie');
CALL upsertStop(60001, N'Edif. 4 Esime', 19.499880164643, -99.1360368102304, N'Edif. 6 Esiqie');
CALL upsertStop(60001, N'Edif. 2 Esime', 19.498560162195, -99.1363197378838, N'Edif. 4 Esime');
CALL upsertStop(60001, N'Edif. 1 Esime', 19.4978416589043, -99.1364507294133, N'Edif. 2 Esime');
CALL upsertStop(60001, N'Terminal Zacatenco', 19.4953797626839, -99.1360002743384, N'Edif. 1 Esime');

INSERT `Incident` VALUES (1, 60001, N'Muerte del conductor', 19.4953797626839, -99.1360002743384);
INSERT `Driver_Incident` VALUES (1, 1, 10001);

INSERT `Vehicle_Route` VALUES (70001, 60001, 40001);
INSERT `Vehicle_Route` VALUES (70002, 60001, 40002);
INSERT `Vehicle_Route` VALUES (70003, 60001, 40003);
INSERT `Vehicle_Route` VALUES (70004, 60001, 40004);
INSERT `Vehicle_Route` VALUES (70005, 60001, 40005);

SET foreign_key_checks = 1;