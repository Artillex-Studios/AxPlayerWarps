package com.artillexstudios.axplayerwarps.libraries;


import com.artillexstudios.axapi.libs.libby.Library;
import com.artillexstudios.axapi.libs.libby.relocation.Relocation;

public enum Libraries {

    HIKARICP("com{}zaxxer:HikariCP:5.1.0", new Relocation("com{}zaxxer{}hikari", "com.artillexstudios.axplayerwarps.libs.hikari")),

    MYSQL_CONNECTOR("com{}mysql:mysql-connector-j:8.0.33", new Relocation("com{}mysql", "com.artillexstudios.axplayerwarps.libs.mysql")),

    SQLITE("org{}xerial:sqlite-jdbc:3.42.0.0"),

    H2_JDBC("com{}h2database:h2:2.1.214", new Relocation("org{}h2", "com.artillexstudios.axplayerwarps.libs.h2database")),

    POSTGRESQL("org{}postgresql:postgresql:42.5.4", new Relocation("org{}postgresql", "com.artillexstudios.axplayerwarps.libs.postgresql")),

    COMMONS_COLLECTIONS("org{}apache{}commons:commons-collections4:4.5.0-M2", new Relocation("org{}apache{}commons{}collections4", "com.artillexstudios.axplayerwarps.libs.apachecollections4"));

    private final Library library;

    public Library getLibrary() {
        return this.library;
    }

    Libraries(String lib, Relocation relocation) {
        String[] split = lib.split(":");

        library = Library.builder()
                .groupId(split[0])
                .artifactId(split[1])
                .version(split[2])
                .relocate(relocation)
                .build();
    }

    Libraries(String lib) {
        String[] split = lib.split(":");

        library = Library.builder()
                .groupId(split[0])
                .artifactId(split[1])
                .version(split[2])
                .build();
    }
}
