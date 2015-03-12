# --- !Ups


CREATE TABLE "weapon" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,
  "name" VARCHAR NOT NULL,
  "kind" INTEGER NOT NULL,
  "speed" INTEGER NOT NULL,
  "power" INTEGER NOT NULL,
  "size" INTEGER NOT NULL);

INSERT INTO weapon (id, name, kind, speed, power, size) VALUES (1, "Single Shot", 1, 80, 100, 100);
INSERT INTO weapon (id, name, kind, speed, power, size) VALUES (2, "Triple Shot", 2, 100, 50, 50);


# --- !Downs


DROP TABLE weapon;