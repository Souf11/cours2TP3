package com.reservations.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entité représentant une salle de réunion ou de travail.
 * Possède des relations avec Equipement (ManyToMany) et Reservation (OneToMany).
 */
@Entity
@Table(name = "salles")
public class Salle implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "capacite")
    private int capacite;

    @Column(name = "etage")
    private int etage;

    /**
     * Relation ManyToMany propriétaire vers Equipement.
     * Table de jointure explicite 'salle_equipements'.
     * Cascades PERSIST et MERGE uniquement : on ne veut JAMAIS CascadeType.REMOVE
     * car supprimer une salle ne doit pas détruire les équipements réutilisables.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "salle_equipements",
            joinColumns = @JoinColumn(name = "salle_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id")
    )
    private Set<Equipement> equipements = new HashSet<>();

    /**
     * Relation OneToMany vers Reservation.
     * Une salle peut accueillir plusieurs réservations au fil du temps.
     */
    @OneToMany(mappedBy = "salle")
    private Set<Reservation> reservations = new HashSet<>();

    public Salle() {
    }

    public Salle(String nom) {
        this.nom = nom;
    }

    public Salle(String code, String nom, int capacite, int etage) {
        this.code = code;
        this.nom = nom;
        this.capacite = capacite;
        this.etage = etage;
    }

    // Méthodes de synchronisation bidirectionnelle pour Equipement
    public void addEquipement(Equipement equipement) {
        if (equipement != null) {
            this.equipements.add(equipement);
            equipement.getSalles().add(this);
        }
    }

    public void removeEquipement(Equipement equipement) {
        if (equipement != null) {
            this.equipements.remove(equipement);
            equipement.getSalles().remove(this);
        }
    }

    // Méthodes de synchronisation bidirectionnelle pour Reservation
    public void addReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservations.add(reservation);
            reservation.setSalle(this);
        }
    }

    public void removeReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservations.remove(reservation);
            reservation.setSalle(null);
        }
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public int getEtage() {
        return etage;
    }

    public void setEtage(int etage) {
        this.etage = etage;
    }

    public Set<Equipement> getEquipements() {
        return equipements;
    }

    public void setEquipements(Set<Equipement> equipements) {
        this.equipements = equipements;
    }

    public Set<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(Set<Reservation> reservations) {
        this.reservations = reservations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Salle salle = (Salle) o;
        if (code != null && salle.code != null) {
            return Objects.equals(code, salle.code);
        }
        return Objects.equals(id, salle.id);
    }

    @Override
    public int hashCode() {
        return code != null ? Objects.hash(code) : (id != null ? Objects.hash(id) : 31);
    }

    @Override
    public String toString() {
        return "Salle{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", nom='" + nom + '\'' +
                ", capacite=" + capacite +
                ", etage=" + etage +
                '}';
    }
}
