package com.reservations.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entité représentant un utilisateur du système de réservation.
 * Relation OneToMany avec Reservation, avec cascade ALL et orphanRemoval = true.
 */
@Entity
@Table(name = "utilisateurs")
public class Utilisateur implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 80)
    private String nom;

    @Column(length = 80)
    private String prenom;

    @Column(length = 100)
    private String departement;

    /**
     * Relation OneToMany vers Reservation.
     * cascade = CascadeType.ALL : propage toutes les opérations (persist, merge, remove, refresh, detach).
     * orphanRemoval = true : si une Reservation est retirée du Set 'reservations',
     *                        Hibernate génère automatiquement un DELETE SQL pour la supprimer en base.
     */
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservations = new HashSet<>();

    public Utilisateur() {
    }

    public Utilisateur(String email, String nom) {
        this.email = email;
        this.nom = nom;
    }

    public Utilisateur(String email, String nom, String prenom, String departement) {
        this.email = email;
        this.nom = nom;
        this.prenom = prenom;
        this.departement = departement;
    }

    /**
     * Méthode utilitaire d'association assurant la cohérence bidirectionnelle.
     * Ajoute la réservation à la collection de l'utilisateur ET associe l'utilisateur à la réservation.
     */
    public void addReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservations.add(reservation);
            reservation.setUtilisateur(this);
        }
    }

    /**
     * Méthode utilitaire de désassociation assurant la cohérence bidirectionnelle.
     * Grâce à orphanRemoval = true, retirer la réservation de la collection
     * provoquera sa suppression physique lors du commit de la transaction.
     */
    public void removeReservation(Reservation reservation) {
        if (reservation != null) {
            this.reservations.remove(reservation);
            reservation.setUtilisateur(null);
        }
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getDepartement() {
        return departement;
    }

    public void setDepartement(String departement) {
        this.departement = departement;
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
        Utilisateur that = (Utilisateur) o;
        if (email != null && that.email != null) {
            return Objects.equals(email, that.email);
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return email != null ? Objects.hash(email) : (id != null ? Objects.hash(id) : 31);
    }

    @Override
    public String toString() {
        return "Utilisateur{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", departement='" + departement + '\'' +
                '}';
    }
}
