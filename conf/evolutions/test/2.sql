# --- !Ups


CREATE TABLE "person" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE,
  "name" VARCHAR NOT NULL,
  "life" INTEGER NOT NULL,
  "speed" INTEGER NOT NULL,
  "size" INTEGER NOT NULL,
  "distance" INTEGER NOT NULL);

INSERT INTO person (id, name, life, speed, size, distance) VALUES (1, "Small", 50, 100, 60, 100);
INSERT INTO person (id, name, life, speed, size, distance) VALUES (2, "Medium", 70, 70, 80, 70);
INSERT INTO person (id, name, life, speed, size, distance) VALUES (3, "Big", 100, 50, 100, 50);


# --- !Downs


DROP TABLE person;