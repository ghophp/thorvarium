# --- !Ups


CREATE TABLE "user" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, "nickname" VARCHAR NOT NULL, "password" VARCHAR NOT NULL);

INSERT INTO user (id, nickname, password) VALUES (1, "[TT]Muro", "4297f44b13955235245b2497399d7a93");


# --- !Downs


DROP TABLE user;