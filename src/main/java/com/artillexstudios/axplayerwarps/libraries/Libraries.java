package com.artillexstudios.axplayerwarps.libraries;


import revxrsal.zapper.Dependency;
import revxrsal.zapper.relocation.Relocation;

import java.util.ArrayList;
import java.util.List;

public enum Libraries {

    HIKARICP("com{}zaxxer:HikariCP:5.1.0", relocation("com{}zaxxer{}hikari", "com.artillexstudios.axplayerwarps.libs.hikari")),

    MYSQL_CONNECTOR("com{}mysql:mysql-connector-j:8.0.33", new Relocation("com{}mysql", "com.artillexstudios.axplayerwarps.libs.mysql")),

    SQLITE("org{}xerial:sqlite-jdbc:3.42.0.0"),

    H2_JDBC("com{}h2database:h2:2.1.214", new Relocation("org{}h2", "com.artillexstudios.axplayerwarps.libs.h2database")),

    POSTGRESQL("org{}postgresql:postgresql:42.5.4", new Relocation("org{}postgresql", "com.artillexstudios.axplayerwarps.libs.postgresql")),

    COMMONS_COLLECTIONS("org{}apache{}commons:commons-collections4:4.5.0-M2", new Relocation("org{}apache{}commons{}collections4", "com.artillexstudios.axplayerwarps.libs.apachecollections4"));

    private final List<Relocation> relocations = new ArrayList<>();
    private final Dependency library;

    public Dependency fetchLibrary() {
        return this.library;
    }

    private static Relocation relocation(String from, String to) {
        return new Relocation(from.replace("{}", "."), to);
    }

    public List<Relocation> relocations() {
        return List.copyOf(this.relocations);
    }

    Libraries(String lib, Relocation relocation) {
        String[] split = lib.replace("{}", ".").split(":");

        this.library = new Dependency(split[0], split[1], split[2]);
        this.relocations.add(relocation);
    }

    Libraries(String lib) {
        String[] split = lib.replace("{}", ".").split(":");

        this.library = new Dependency(split[0], split[1], split[2]);
    }
}
