package ch.so.agi.stats;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Fruit extends PanacheEntity {

    @Column(length = 40, unique = true)
    public String name;
    public double price;

    public Fruit() {
    }

    public Fruit(String name) {
        this.name = name;
    }
}
